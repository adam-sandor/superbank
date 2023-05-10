minikube start \
  --profile banking-demo \
  --driver=docker \
  --container-runtime=containerd \
  --cpus 3 \
  --memory=6144 \
  --extra-config=apiserver.enable-admission-plugins=MutatingAdmissionWebhook,ValidatingAdmissionWebhook
