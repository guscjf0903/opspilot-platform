package com.opspilot.common.exception;

import com.opspilot.ai.application.AnalysisTargetNotFoundException;
import com.opspilot.ai.application.AiProviderException;
import com.opspilot.ai.application.IncidentAnalysisNotFoundException;
import com.opspilot.ai.application.UnsupportedAnalysisTargetException;
import com.opspilot.action.application.ActionApprovalNotFoundException;
import com.opspilot.action.application.ActionForbiddenException;
import com.opspilot.action.application.ActionTargetNotFoundException;
import com.opspilot.action.application.ActionValidationException;
import com.opspilot.action.application.KubernetesActionException;
import com.opspilot.action.application.UnsupportedActionException;
import com.opspilot.kubernetes.application.KubernetesApiException;
import com.opspilot.kubernetes.application.UnknownClusterException;
import com.opspilot.metrics.application.UnsupportedMetricResourceException;
import com.opspilot.topology.application.TopologyResourceNotFoundException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownClusterException.class)
    public ResponseEntity<ApiError> handleUnknownCluster(UnknownClusterException exception) {
        return response(HttpStatus.NOT_FOUND, "CLUSTER_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(KubernetesApiException.class)
    public ResponseEntity<ApiError> handleKubernetesApi(KubernetesApiException exception) {
        return response(HttpStatus.BAD_GATEWAY, "KUBERNETES_API_ERROR", exception.getMessage());
    }

    @ExceptionHandler(TopologyResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleTopologyResourceNotFound(TopologyResourceNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "TOPOLOGY_RESOURCE_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(UnsupportedMetricResourceException.class)
    public ResponseEntity<ApiError> handleUnsupportedMetricResource(UnsupportedMetricResourceException exception) {
        return response(HttpStatus.BAD_REQUEST, "UNSUPPORTED_METRIC_RESOURCE", exception.getMessage());
    }

    @ExceptionHandler(UnsupportedAnalysisTargetException.class)
    public ResponseEntity<ApiError> handleUnsupportedAnalysisTarget(UnsupportedAnalysisTargetException exception) {
        return response(HttpStatus.BAD_REQUEST, "UNSUPPORTED_ANALYSIS_TARGET", exception.getMessage());
    }

    @ExceptionHandler(AnalysisTargetNotFoundException.class)
    public ResponseEntity<ApiError> handleAnalysisTargetNotFound(AnalysisTargetNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "ANALYSIS_TARGET_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(IncidentAnalysisNotFoundException.class)
    public ResponseEntity<ApiError> handleIncidentAnalysisNotFound(IncidentAnalysisNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "INCIDENT_ANALYSIS_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ApiError> handleAiProvider(AiProviderException exception) {
        return response(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR", exception.getMessage());
    }

    @ExceptionHandler(ActionForbiddenException.class)
    public ResponseEntity<ApiError> handleActionForbidden(ActionForbiddenException exception) {
        return response(HttpStatus.FORBIDDEN, "ACTION_FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler({
            ActionValidationException.class,
            UnsupportedActionException.class
    })
    public ResponseEntity<ApiError> handleActionValidation(RuntimeException exception) {
        return response(HttpStatus.BAD_REQUEST, "ACTION_VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(ActionTargetNotFoundException.class)
    public ResponseEntity<ApiError> handleActionTargetNotFound(ActionTargetNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "ACTION_TARGET_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ActionApprovalNotFoundException.class)
    public ResponseEntity<ApiError> handleActionApprovalNotFound(ActionApprovalNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "ACTION_APPROVAL_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(KubernetesActionException.class)
    public ResponseEntity<ApiError> handleKubernetesAction(KubernetesActionException exception) {
        return response(HttpStatus.BAD_GATEWAY, "KUBERNETES_ACTION_ERROR", exception.getMessage());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, Instant.now()));
    }
}
