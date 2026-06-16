package com.opspilot.action.application.port.out;

import com.opspilot.action.domain.ActionCommand;
import com.opspilot.action.domain.ActionPreview;

public interface KubernetesActionPort {

    ActionPreview preview(ActionCommand command);

    ActionPreview execute(ActionCommand command);
}
