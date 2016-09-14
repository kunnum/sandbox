for i in London Cardiff Birmingham Glasgow Belfast
do
  for j in {1..10}
  do
    curl --data "$i,$j,$((10*j + RANDOM % 10))" http://localhost:8080 &
  done
done