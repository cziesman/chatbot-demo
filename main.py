from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
from langchain_community.vectorstores import FAISS
from langchain_community.embeddings import SentenceTransformerEmbeddings
from langchain.chains import RetrievalQA
from langchain_community.chat_models import ChatOpenAI  # or use HuggingFaceHub, ChatLiteLLM, etc.
from langchain.schema import HumanMessage
from pydantic import BaseModel

import os

import pkg_resources
print("INSTALLED PACKAGES:")
print([pkg.key for pkg in pkg_resources.working_set])

app = FastAPI()

# Load index and build the chatbot once at startup
INDEX_DIR = "/workspace/shared-storage/index"

try:
    embedder = SentenceTransformerEmbeddings(model_name="all-MiniLM-L6-v2")
    vectorstore = FAISS.load_local(INDEX_DIR, embedder, allow_dangerous_deserialization=True)
    retriever = vectorstore.as_retriever()
    llm = ChatOpenAI(
        model="granite-13b-chat",
        openai_api_key=os.getenv("OPENAI_API_KEY"),
        openai_api_base=os.getenv("OPENAI_BASE_URL"),
        temperature=0.5
    )
    chatbot = RetrievalQA.from_chain_type(llm=llm, retriever=retriever)
except Exception as e:
    print(f"Failed to initialize chatbot: {e}")
    chatbot = None

class ChatRequest(BaseModel):
    question: str

@app.post("/chat")
async def chat_endpoint(request: ChatRequest):
    try:
        response = llm([HumanMessage(content=request.question)])
        return {"response": response.content}
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)})