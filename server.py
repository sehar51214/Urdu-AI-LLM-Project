from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from llama_cpp import Llama
from typing import List, Optional
import uvicorn
import re

# Initialize FastAPI app
app = FastAPI(title="Urdu AI API", description="Backend API for Urdu LLM")

# Enable CORS for frontend integration (Android/Web)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ==========================================
# MODEL CONFIGURATION
# ==========================================
# Path to the quantized GGUF model
MODEL_PATH = "urdu_8b_final.gguf" 

print("Initializing Urdu LLM...")
try:
    # Load model with optimized settings for CPU
    llm = Llama(
        model_path=MODEL_PATH,
        n_ctx=2048,       # Context window size
        n_threads=8,      # Number of CPU threads
        n_batch=128,      # Batch size for prompt processing
        verbose=False,
    )
    print("Model loaded successfully.")
except Exception as e:
    print(f"Error loading model: {e}")
    raise RuntimeError("Failed to initialize the LLM.")

# ==========================================
# DATA SCHEMAS
# ==========================================
class ChatMessage(BaseModel):
    role: str      # 'user' or 'assistant'
    content: str

class ChatRequest(BaseModel):
    message: str
    history: Optional[List[ChatMessage]] = []

# ==========================================
# API ENDPOINTS
# ==========================================
@app.post("/chat")
def chat_endpoint(req: ChatRequest) -> dict:
    """
    Handles incoming chat requests, constructs context, and generates responses.
    """
    user_message = req.message.strip()
    if not user_message:
        raise HTTPException(status_code=400, detail="Message cannot be empty.")

    # 1. Dynamic Token Limit Extraction (Words/Lines)
    max_tokens = 500  # Default limit
    length_instruction = ""
    
    word_match = re.search(r'(\d+)\s*(word|words|alfaz|لفظ|الفاظ)', user_message, re.IGNORECASE)
    line_match = re.search(r'(\d+)\s*(line|lines|satrain|سطر|سطریں)', user_message, re.IGNORECASE)

    if word_match:
        word_count = int(word_match.group(1))
        max_tokens = int(word_count * 2.5)
        length_instruction = f"\n\nInstruction: The user requested {word_count} words. Write a detailed essay."
    elif line_match:
        line_count = int(line_match.group(1))
        max_tokens = int(line_count * 25)
        length_instruction = f"\n\nInstruction: The user requested {line_count} lines. Ensure proper line breaks."

    # 2. System Prompt Configuration
    system_prompt = (
        "You are UrduAI, a highly capable Urdu language assistant. "
        "You must reply ONLY in pure and literary Urdu. "
        "Provide accurate factual information. Do not hallucinate or create false facts. "
        "If asked for poetry or essays, generate creative and diverse vocabulary without repetition."
        + length_instruction
    )

    # 3. Context Construction (Llama-3 Chat Template)
    prompt = f"<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n{system_prompt}<|eot_id|>"
    
    # Append conversation history for memory
    for msg in req.history:
        prompt += f"<|start_header_id|>{msg.role}<|end_header_id|>\n\n{msg.content}<|eot_id|>"
        
    # Append current user message
    prompt += f"<|start_header_id|>user<|end_header_id|>\n\n{user_message}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n"
    
    # 4. Model Inference
    try:
        response = llm(
            prompt, 
            max_tokens=max_tokens, 
            temperature=0.7,       # Creativity level
            top_p=0.9, 
            repeat_penalty=1.2,    # Prevents repetitive loops
            stop=["<|eot_id|>"]
        )
        
        generated_text = response['choices'][0]['text'].strip()
        return {"response": generated_text}
        
    except Exception as e:
        print(f"Inference Error: {e}")
        raise HTTPException(status_code=500, detail="Error generating response.")

@app.get("/")
def health_check() -> dict:
    """Health check endpoint to verify server status."""
    return {"status": "active", "model": "Urdu-LLM-8B"}

# ==========================================
# ENTRY POINT
# ==========================================
if __name__ == "__main__":
    # Start server on all interfaces, port 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)