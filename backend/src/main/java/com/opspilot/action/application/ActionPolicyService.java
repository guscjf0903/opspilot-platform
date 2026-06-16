package com.opspilot.action.application;

import com.opspilot.action.domain.ActionCommand;
import com.opspilot.action.domain.ActionRiskLevel;
import com.opspilot.action.domain.ActionType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ActionPolicyService {

    public ActionRiskLevel risk(ActionCommand command, boolean productionNamespace) {
        if (command.type() == ActionType.ROLLOUT_UNDO || command.type() == ActionType.DELETE_POD) {
            return productionNamespace ? ActionRiskLevel.HIGH : ActionRiskLevel.MEDIUM;
        }

        if (command.type() == ActionType.SCALE_DEPLOYMENT) {
            int replicas = replicas(command);
            if (replicas == 0 || productionNamespace) {
                return ActionRiskLevel.MEDIUM;
            }
        }

        return productionNamespace ? ActionRiskLevel.MEDIUM : ActionRiskLevel.LOW;
    }

    public List<String> warnings(ActionCommand command, boolean productionNamespace) {
        List<String> warnings = new ArrayList<>();

        if (productionNamespace) {
            warnings.add("production namespace 조치는 승인 또는 추가 확인이 필요합니다.");
        }

        if (command.type() == ActionType.SCALE_DEPLOYMENT && replicas(command) == 0) {
            warnings.add("replica 0 설정은 서비스를 중단할 수 있습니다.");
        }

        if (command.type() == ActionType.ROLLOUT_UNDO) {
            warnings.add("rollback은 이전 ReplicaSet template으로 Deployment를 되돌립니다.");
        }

        if (command.type() == ActionType.DELETE_POD) {
            warnings.add("Pod 삭제는 owner가 있는 경우 새 Pod 재생성을 유도합니다.");
        }

        return warnings;
    }

    public void validate(ActionCommand command, boolean productionNamespace) {
        if (command.type() == ActionType.SCALE_DEPLOYMENT) {
            int replicas = replicas(command);
            if (replicas < 0) {
                throw new ActionValidationException("replicas must be greater than or equal to 0.");
            }
            if (productionNamespace && replicas < 1) {
                throw new ActionValidationException("production namespace cannot be scaled below 1 replica.");
            }
        }
    }

    public int replicas(ActionCommand command) {
        String raw = command.parameters().get("replicas");
        if (raw == null || raw.isBlank()) {
            throw new ActionValidationException("replicas parameter is required for SCALE_DEPLOYMENT.");
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException exception) {
            throw new ActionValidationException("replicas must be an integer.");
        }
    }
}
