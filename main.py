from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
from langchain_community.vectorstores import FAISS
from langchain_community.embeddings import SentenceTransformerEmbeddings
from langchain.chains import RetrievalQA
from langchain.chat_models import ChatOpenAI
from langchain.schema import HumanMessage
from pydantic import BaseModel
import httpx

import os

import pkg_resources
print("INSTALLED PACKAGES:")
print([pkg.key for pkg in pkg_resources.working_set])

app = FastAPI()

# Load index and build the chatbot once at startup
INDEX_DIR = "/workspace/shared-storage/index"

try:
    # Disable cert check by creating a custom httpx Client
    http_client = httpx.Client(verify=False)

    embedder = SentenceTransformerEmbeddings(model_name="all-MiniLM-L6-v2")
    vectorstore = FAISS.load_local(INDEX_DIR, embedder, allow_dangerous_deserialization=True)
    retriever = vectorstore.as_retriever()

#     llm = ChatGranite(
#         model="granite-13b-chat",
#         openai_api_key=os.getenv("OPENAI_API_KEY"),
#         base_url=os.getenv("OPENAI_BASE_URL"),
#         model_kwargs={"temperature": 0.5},
#         http_client=http_client
#     )
    llm = ChatOpenAI(
        openai_api_base=os.getenv("OPENAI_BASE_URL"),
        openai_api_key=os.getenv("OPENAI_API_KEY"),
        model="granite-13b-chat",
        temperature=0.5,
        http_client=http_client
    )
    chatbot = RetrievalQA.from_chain_type(llm=llm, retriever=retriever)
except Exception as e:
    print(f"Failed to initialize chatbot: {e}")
    chatbot = None

class ChatRequest(BaseModel):
    question: str

@app.post("/chat")
def chat_endpoint(request: ChatRequest):
    try:
        if chatbot is None:
            raise RuntimeError("Chatbot is not initialized.")

        print(f"Sending to LLM: {request.question}")
        response = llm.invoke([HumanMessage(content=request.question)])
        print(f"LLM responded: {response}")
        return {"response": response["result"]}
    except Exception as e:
        import traceback
        traceback.print_exc()
        return JSONResponse(status_code=500, content={"error": str(e)})
