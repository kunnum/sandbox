for i in {1..5}
do
  for j in {1..10}
  do
    curl --data "$i,$j,$((i * 10 + RANDOM % 10))" http://localhost:8080 &
  done
done