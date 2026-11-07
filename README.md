# 🇵🇰 Project Alif: End-to-End Urdu LLM Application

An advanced, fully functional Generative AI application featuring a fine-tuned **Llama 3.1 (8B & 3B)** model, a local **FastAPI** backend, and an **Android App (Jetpack Compose)** with native Voice capabilities. 

This project was built to provide a 100% offline, privacy-focused, pure Urdu AI assistant.

## 🚀 Key Features
*   **Custom Fine-Tuning:** Fine-tuned Llama 3.1 (8B & 3B) using **Unsloth** and **LoRA** on a custom dataset of 156,000+ pure Urdu samples.
*   **100% Offline Inference:** Deployed locally using **FastAPI** and `llama-cpp` (GGUF Q4_K_M quantization) with zero reliance on paid Cloud APIs.
*   **Voice-Enabled Frontend:** Android App built with **Kotlin & Jetpack Compose**, featuring native Speech-to-Text (STT) and Text-to-Speech (TTS).
*   **Conversational Memory:** Integrated context window management to remember previous user interactions.
*   **Dynamic Prompt Engineering:** Extracts user constraints (e.g., "write 100 words") and dynamically adjusts `max_tokens` for controlled generation.
*   **Local Database:** Utilizes **SQLite (Room)** for secure, offline chat history management.

## 🛠️ Tech Stack & Skills Demonstrated
*   **AI / Machine Learning:** Python, PyTorch, Unsloth, Hugging Face (Transformers, PEFT), LoRA, GGUF Quantization.
*   **Backend Engineering:** FastAPI, Uvicorn, REST APIs, Pydantic, Object-Oriented Programming (OOP).
*   **Android Development:** Kotlin, Jetpack Compose, Coroutines, Retrofit (REST Client), Room Database.
*   **Algorithms & Logic:** Regex parsing for token constraints, Context management, Database Migrations.

## 🏗️ System Architecture
1.  **User Input:** Android App takes text or voice input (STT).
2.  **API Request:** App sends an HTTP POST request (with chat history) to the Local FastAPI server.
3.  **Inference:** FastAPI triggers the `llama-cpp` model (GGUF format).
4.  **Response:** The generated Urdu text is returned as JSON, saved to SQLite, and displayed on the UI.
5.  **Voice Output:** User can tap the speaker icon to hear the response (TTS).

## 📊 Model Training & Evaluation
*   **Dataset Size:** 156,905 curated Urdu samples (Poetry, Q&A, Conversational).
*   **Training Loss:** Reduced from 1.6 to **0.61** (Stable convergence).
*   **Validation Loss:** **0.62** (No overfitting).
*   *(See the attached loss graph for detailed training metrics)*.

## 🚀 Future Work
*   Integrating **RAG (Retrieval-Augmented Generation)** for factual accuracy.
*   Expanding the dataset to 500k+ samples.
*   Scaling to Cloud (AWS/GCP) for public Play Store deployment.

## 📸 App Screenshots
<img width="1920" height="1008" alt="Screenshot 2026-10-31 161557" src="https://github.com/user-attachments/assets/0f2e0414-a913-4456-ba0d-0c16e2dea697" />
<img width="1920" height="1008" alt="Screenshot 2026-10-31 161557" src="https://github.com/user-attachments/assets/3959e6c6-8866-42eb-9b60-1ceba93b44d3" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-07 at 4 52 16 PM" src="https://github.com/user-attachments/assets/d5ff9ca2-d1b2-48b4-9397-0aaa0f51542f" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-07 at 4 52 16 PM" src="https://github.com/user-attachments/assets/fa8bedcf-976d-40b3-ac4c-cbfebdcf4371" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-07 at 4 51 16 PM" src="https://github.com/user-attachments/assets/425300c2-7140-4dc3-81c6-75948f771bd0" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-07 at 4 51 16 PM (1)" src="https://github.com/user-attachments/assets/7e1e46d0-2535-42ae-9824-92f578d6b315" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-07 at 4 51 16 PM (2)" src="https://github.com/user-attachments/assets/f6a4fc00-b559-4533-aa67-1220453deef3" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-07 at 4 51 15 PM" src="https://github.com/user-attachments/assets/4e381cfd-bab7-4102-8b5a-3e9a8fd3aa43" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-07 at 4 51 15 PM (1)" src="https://github.com/user-attachments/assets/e80a8e56-7b36-45eb-8b18-ceb8454f5935" />
