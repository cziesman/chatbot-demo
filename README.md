# chatbot-demo

curl --insecure https://chatbot-compliance-chatbot.apps.cluster-4r95f.4r95f.sandbox481.opentlc.com/chat \\n-H "Content-Type: application/json" \\n-d '{"question": "what is the most important consumer compliance rule?"}'

curl http://localhost:8080/chat -H "Content-Type: application/json" -d '{"question": "what is the most important consumer compliance rule?", "temperature": 0.7}'


curl http://localhost:8080/v1/chat/completions/v1/chat/completions \
-H "Content-Type: application/json" \
-d '{
"model": "granite",
"messages": [{"role": "user", "content": "Hello!"}],
"temperature": 0.7
}'


curl -X POST http://localhost:8080/chat \
-H "Content-Type: application/json" \
-d '{
"messages": [
{ "role": "user", "content": "what is the most important consumer compliance rule?" }
],
"temperature": 0.7,
"max_tokens": 256
}'


curl http://localhost:8080/openapi.json

curl -X POST http://localhost:8080/chat -H "Content-Type: application/json" -d '{"question": "hello"}'
