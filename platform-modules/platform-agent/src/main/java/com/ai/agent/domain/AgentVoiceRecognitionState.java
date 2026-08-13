package com.ai.agent.domain;

/**
 * 语音转写记录的处理状态。
 *
 * @author guanxiangkai
 * @since 1.0.0
 */
public enum AgentVoiceRecognitionState {
    RUNNING,
    SUCCEEDED,
    NEED_REVIEW,
    FAILED
}
