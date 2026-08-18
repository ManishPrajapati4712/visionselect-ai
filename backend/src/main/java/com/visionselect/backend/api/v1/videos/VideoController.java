package com.visionselect.backend.api.v1.videos;

import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.common.response.ApiResponse;
import com.visionselect.backend.common.util.ApiPaths;
import com.visionselect.backend.video.dto.UploadUrlRequest;
import com.visionselect.backend.video.dto.UploadUrlResponse;
import com.visionselect.backend.video.dto.VideoCreateRequest;
import com.visionselect.backend.video.dto.VideoResponse;
import com.visionselect.backend.video.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Only the two-step upload flow from the Videos tag in openapi.yaml is
 * implemented in this phase: {@code POST /videos/upload-url} and
 * {@code POST /videos}. {@code GET /videos}, {@code GET /videos/{videoId}},
 * and {@code DELETE /videos/{videoId}} are documented in the contract but
 * out of scope here.
 *
 * <p>{@code @PreAuthorize("hasAnyRole('ADMIN','COACH')")} on both methods
 * is the RBAC narrowing security-contract.md's table requires for the
 * "Videos — upload/create" row (yes/yes/no/no for
 * ADMIN/COACH/SELECTOR/PLAYER) - {@code SecurityConfig} itself only
 * requires *some* authenticated principal for this path; the role check
 * happens here, per that class's own javadoc on where narrowing belongs.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/upload-url")
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    public ApiResponse<UploadUrlResponse> requestUploadUrl(@AuthenticationPrincipal AuthenticatedUser principal,
                                                             @Valid @RequestBody UploadUrlRequest request) {
        return ApiResponse.success(videoService.requestUploadUrl(principal.userId(), request));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VideoResponse> registerUpload(@AuthenticationPrincipal AuthenticatedUser principal,
                                                       @Valid @RequestBody VideoCreateRequest request) {
        return ApiResponse.success(videoService.registerUpload(principal.userId(), request), "Video registered");
    }
}
