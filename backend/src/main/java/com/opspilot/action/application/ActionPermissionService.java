package com.opspilot.action.application;

import com.opspilot.action.domain.ActionActor;
import com.opspilot.action.domain.ActionCommand;
import com.opspilot.action.domain.ActionType;
import com.opspilot.action.domain.UserRole;
import org.springframework.stereotype.Service;

@Service
public class ActionPermissionService {

    public void assertCanRequest(ActionActor actor, ActionCommand command) {
        if (actor.role() == UserRole.VIEWER) {
            throw new ActionForbiddenException("Viewer role cannot request Kubernetes actions.");
        }

        if (actor.role() == UserRole.DEVELOPER) {
            assertDeveloperScope(command);
        }
    }

    public boolean approvalRequired(ActionActor actor, ActionCommand command) {
        if (actor.role() == UserRole.ADMIN) {
            return false;
        }

        return isProductionNamespace(command.namespace())
                || command.type() == ActionType.ROLLOUT_UNDO;
    }

    public boolean isProductionNamespace(String namespace) {
        String normalized = namespace == null ? "" : namespace.trim().toLowerCase();
        return normalized.equals("prod")
                || normalized.equals("production")
                || normalized.endsWith("-prod")
                || normalized.startsWith("prod-");
    }

    private void assertDeveloperScope(ActionCommand command) {
        if (isProductionNamespace(command.namespace())) {
            throw new ActionForbiddenException("Developer role cannot execute actions in production namespaces.");
        }

        if (command.type() == ActionType.ROLLOUT_UNDO) {
            throw new ActionForbiddenException("Developer role cannot execute rollout undo.");
        }
    }
}
