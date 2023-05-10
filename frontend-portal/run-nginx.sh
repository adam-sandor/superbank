docker run \
  -v $(pwd)/default.conf:/etc/nginx/conf.d/default.conf \
  -v $(pwd)/html:/usr/share/nginx/html \
  -p 80:80 nginx