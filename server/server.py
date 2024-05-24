from flask import Flask, request, jsonify
from flask_sslify import SSLify
from dotenv import load_dotenv
import ssl
import requests
import os

load_dotenv('api_key.env')

app = Flask(__name__)
sslify = SSLify(app)

CHATGPT_API_KEY = os.getenv('CHATGPT_API_KEY')
CHATGPT_API_URL = "https://api.openai.com/v1/chat/completions"


@app.route('/process_message', methods=['POST'])
def process_message():
    try:
        # Получаем текст сообщения от приложения пользователя
        user_message = request.json['message']
        prompt = request.json['prompt']
        print(user_message)
        print(prompt)
        # Отправляем запрос к ChatGPT через API
        ai_response = get_ai_response(user_message, prompt)
        print(ai_response)
        # Возвращаем ответ в приложение пользователя1
        return jsonify({'response': ai_response})
    except Exception as e:
        return jsonify('response :  error on the server')


def get_ai_response(user_message, prompt):
    try:
        headers = {
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {CHATGPT_API_KEY}'
        }

        data = {
            'model': 'gpt-3.5-turbo',
            'messages': [
                {'role': 'system', 'content': prompt},
                {'role': 'user', 'content': user_message}
            ]
        }

        response = requests.post(CHATGPT_API_URL, json=data, headers=headers)
        response.raise_for_status()

        ai_response = response.json()['choices'][0]['message']['content']
        return ai_response
    except Exception as e:
        raise RuntimeError(f'Error in ChatGPT API request: {str(e)}')


if __name__ == '__main__':
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certfile='cert.pem', keyfile='key.pem')
    app.run(host='0.0.0.0', port=443, ssl_context=context)
