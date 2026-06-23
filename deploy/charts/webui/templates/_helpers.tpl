{{- define "webui.labels" -}}
app.kubernetes.io/part-of: platform
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{- define "webui.console.image" -}}
{{- $reg := .Values.global.imageRegistry | default "" -}}
{{- $img := printf "%s:%s" .Values.console.image.repository (.Values.console.image.tag | default .Chart.AppVersion) -}}
{{- if $reg }}{{ printf "%s/%s" $reg $img }}{{- else }}{{ $img }}{{- end }}
{{- end -}}

{{- define "webui.admin.image" -}}
{{- $reg := .Values.global.imageRegistry | default "" -}}
{{- $img := printf "%s:%s" .Values.admin.image.repository (.Values.admin.image.tag | default .Chart.AppVersion) -}}
{{- if $reg }}{{ printf "%s/%s" $reg $img }}{{- else }}{{ $img }}{{- end }}
{{- end -}}
