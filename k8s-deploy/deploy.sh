K8S_NAMESPACE=banking-demo

istioctl install --set profile=default -y

kubectl create ns $K8S_NAMESPACE
kubectl ns $K8S_NAMESPACE
kubectl label namespaces $K8S_NAMESPACE istio-injection=enabled

kubectl create secret generic gcs-credentials --from-literal=pk="$(cat gcs-pk.txt)" -n $K8S_NAMESPACE
kubectl create configmap opa-config --from-file=k8s-deploy/opa-conf.yaml -n $K8S_NAMESPACE

kubectl apply -f k8s-deploy/istio-gateway.yaml
kubectl apply -f k8s-deploy/envoy-filter.yaml
kubectl apply -k k8s-deploy/with-opa