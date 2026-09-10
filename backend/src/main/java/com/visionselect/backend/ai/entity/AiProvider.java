package com.visionselect.backend.ai.entity;

/** Which AI provider backs a job. Controls which {@link com.visionselect.backend.ai.provider.AiProviderClient} is used. */
public enum AiProvider {
    /** The Python FastAPI microservice (backed by OpenCV / MediaPipe / YOLO / Gemini). */
    PYTHON_AI_SERVICE,
    /** In-process mock — returns canned results; used in tests and local dev. */
    MOCK
}
