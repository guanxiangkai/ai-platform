"""从实际网关部署 JAR 核验默认日志切面所需的运行时依赖，不加载外部配置。"""
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tempfile
import zipfile

PROBE = """
public class RuntimeDependencyProbe {
    public static void main(String[] args) throws Exception {
        for (String name : args) {
            Class.forName(name, false, ClassLoader.getSystemClassLoader()).getDeclaredMethods();
        }
    }
}
"""
CLASSES = [
    "org.aspectj.lang.ProceedingJoinPoint",
    "org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator",
    "io.github.guanxiangkai.web.plus.log.autoconfigure.WebPlusLogAutoConfiguration",
    "io.github.guanxiangkai.web.plus.log.aspect.OperationLogAspect",
]


def verify(jar: Path) -> None:
    """仅使用 bootJar 内的运行时依赖执行类加载检查。"""
    with tempfile.TemporaryDirectory(prefix="gateway-runtime-") as directory:
        root = Path(directory)
        libraries = root / "lib"
        libraries.mkdir()
        with zipfile.ZipFile(jar) as archive:
            for entry in archive.infolist():
                path = PurePosixPath(entry.filename)
                if path.parts[:2] != ("BOOT-INF", "lib") or path.suffix != ".jar":
                    continue
                if len(path.parts) != 3 or entry.is_dir():
                    raise RuntimeError("网关运行时 JAR 路径异常")
                with archive.open(entry) as source, (libraries / path.name).open("wb") as target:
                    shutil.copyfileobj(source, target, 1024 * 1024)
        probe = root / "RuntimeDependencyProbe.java"
        probe.write_text(PROBE)
        subprocess.run(["java", "--class-path", str(libraries / "*"), str(probe), *CLASSES], check=True)
    print("网关部署包日志切面运行时依赖校验通过")


if __name__ == "__main__":
    verify(Path(sys.argv[1]))
