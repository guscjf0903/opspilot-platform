{{- define "opspilot.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "opspilot.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "opspilot.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "opspilot.labels" -}}
helm.sh/chart: {{ include "opspilot.chart" . }}
app.kubernetes.io/name: {{ include "opspilot.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- with .Values.commonLabels }}
{{- toYaml . | nindent 0 }}
{{- end }}
{{- end -}}

{{- define "opspilot.selectorLabels" -}}
app.kubernetes.io/name: {{ include "opspilot.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "opspilot.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "opspilot.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{- define "opspilot.backendName" -}}
{{- printf "%s-backend" (include "opspilot.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "opspilot.frontendName" -}}
{{- printf "%s-frontend" (include "opspilot.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "opspilot.backendConfigName" -}}
{{- printf "%s-config" (include "opspilot.backendName" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
