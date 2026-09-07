#!/usr/bin/env sh
set -eu

repository=${1:?需要 Maven bundle 目录}
version=${2:?需要 API 版本}
group_path=com/ai/platform

case "$version" in
  *SNAPSHOT*)
    echo "API bundle 版本不得包含 SNAPSHOT: $version" >&2
    exit 1
    ;;
esac

for artifact in platform-core-api platform-system-api platform-files-api platform-agent-api; do
  artifact_dir="$repository/$group_path/$artifact/$version"
  pom="$artifact_dir/$artifact-$version.pom"
  jar="$artifact_dir/$artifact-$version.jar"
  test -f "$pom"
  test -f "$jar"
  jar --list --file "$jar" >/dev/null
done

python3 - "$repository" "$version" <<'PY'
import pathlib, re, sys, xml.etree.ElementTree as ET
repository, version = pathlib.Path(sys.argv[1]), sys.argv[2]
assert re.fullmatch(r'1\.0\.0-[0-9a-f]{40}', version), 'API 版本必须绑定完整提交 ID'
assert not list(repository.rglob('*.module')), 'API bundle 只发布 Maven POM 与 JAR'
artifacts = {'platform-core-api', 'platform-system-api', 'platform-files-api', 'platform-agent-api'}
namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
for artifact in artifacts:
    pom = ET.parse(repository / 'com/ai/platform' / artifact / version / f'{artifact}-{version}.pom').getroot()
    def value(element, name): return element.findtext('m:' + name, namespaces=namespace)
    assert (value(pom, 'groupId'), value(pom, 'artifactId'), value(pom, 'version')) == ('com.ai.platform', artifact, version)
    dependencies = pom.findall('.//m:dependency', namespace)
    for dependency in dependencies:
        dependency_version = value(dependency, 'version') or ''
        assert 'SNAPSHOT' not in dependency_version, 'API 依赖必须使用固定版本'
        if value(dependency, 'groupId') == 'com.ai.platform':
            assert value(dependency, 'artifactId') in artifacts and dependency_version == version, '平台内部 API 版本不一致'
    if artifact != 'platform-core-api':
        assert any(value(d, 'groupId') == 'com.ai.platform' and value(d, 'artifactId') == 'platform-core-api' for d in dependencies)
    if artifact == 'platform-system-api':
        assert any(value(d, 'artifactId') == 'web-plus-log' and value(d, 'scope') in [None, 'compile'] for d in dependencies), '公开日志父类必须传递到消费者编译类路径'
print('API Maven bundle verified: 4 modules, immutable coordinates, Maven-only metadata')
PY

consumer=build/platform-api-binary-consumer
mkdir -p "$consumer/src/main/java"
cat > "$consumer/settings.gradle.kts" <<'GRADLE'
rootProject.name = "platform-api-binary-consumer"
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    url = uri(providers.gradleProperty("apiBundleRepository").get())
                    metadataSources { mavenPom(); artifact() }
                }
            }
            filter { includeGroup("com.ai.platform") }
        }
        mavenCentral()
    }
}
GRADLE
cat > "$consumer/build.gradle.kts" <<'GRADLE'
plugins { java }
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor.set(JvmVendorSpec.ORACLE)
    }
}
dependencies {
    val apiVersion = providers.gradleProperty("apiBundleVersion").get()
    listOf("platform-core-api", "platform-system-api", "platform-files-api", "platform-agent-api").forEach {
        implementation("com.ai.platform:$it:$apiVersion")
    }
}
GRADLE
cat > "$consumer/src/main/java/ApiConsumer.java" <<'JAVA'
import com.ai.api.context.TenantExecutionScope;
import com.ai.api.system.client.SystemClient;
import com.ai.api.files.client.FilesClient;
import com.ai.api.agent.client.AgentClient;
import com.ai.api.system.log.PlatformOperationLog;
import io.github.guanxiangkai.web.plus.log.entity.BaseLog;

/** 仅经 Maven POM/JAR 编译平台公开类型及其公开父类。 */
public final class ApiConsumer {
    public Class<?>[] clientTypes() {
        return new Class<?>[] {TenantExecutionScope.class, SystemClient.class, FilesClient.class, AgentClient.class};
    }
    public BaseLog operationLog() { return new PlatformOperationLog(); }
}
JAVA
./gradlew -p "$consumer" compileJava \
  "-PapiBundleRepository=$(cd "$repository" && pwd)" \
  "-PapiBundleVersion=$version" --no-daemon --stacktrace
