// Chat functionality
class ChatApp {
    constructor() {
        this.chatMessages = document.getElementById('chat-messages');
        this.messageInput = document.getElementById('message-input');
        this.chatForm = document.getElementById('chat-form');
        this.sendButton = document.getElementById('send-button');

        this.initializeEventListeners();
        this.loadChatHistory();
    }

    initializeEventListeners() {
        this.chatForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.sendMessage();
        });

        this.messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });
    }

    async loadChatHistory() {
        try {
            const response = await fetch('/api/chat/history');
            const messages = await response.json();

            messages.forEach(message => {
                this.displayMessage(message.content, message.role, false);
            });
        } catch (error) {
            console.error('Failed to load chat history:', error);
        }
    }

    async sendMessage() {
        const message = this.messageInput.value.trim();
        if (!message) return;

        // Display user message
        this.displayMessage(message, 'user');
        this.messageInput.value = '';
        this.setLoading(true);

        try {
            // Show typing indicator
            this.showTypingIndicator();

            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    content: message,
                    role: 'user'
                })
            });

            if (!response.ok) {
                throw new Error('Failed to send message');
            }

            const assistantMessage = await response.json();

            // Hide typing indicator and show response
            this.hideTypingIndicator();
            this.displayMessage(assistantMessage.content, 'assistant');

        } catch (error) {
            this.hideTypingIndicator();
            this.displayMessage('Sorry, I encountered an error. Please try again.', 'assistant');
            console.error('Error sending message:', error);
        } finally {
            this.setLoading(false);
        }
    }

    displayMessage(content, role, animate = true) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;

        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'message-bubble';
        bubbleDiv.textContent = content;

        const timeDiv = document.createElement('div');
        timeDiv.className = 'message-time';
        timeDiv.textContent = new Date().toLocaleTimeString();

        messageDiv.appendChild(bubbleDiv);
        messageDiv.appendChild(timeDiv);

        if (animate) {
            messageDiv.style.opacity = '0';
            messageDiv.style.transform = 'translateY(10px)';
        }

        this.chatMessages.appendChild(messageDiv);

        if (animate) {
            requestAnimationFrame(() => {
                messageDiv.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
                messageDiv.style.opacity = '1';
                messageDiv.style.transform = 'translateY(0)';
            });
        }

        this.scrollToBottom();
    }

    showTypingIndicator() {
        const typingDiv = document.createElement('div');
        typingDiv.className = 'typing-indicator';
        typingDiv.id = 'typing-indicator';

        const bubbleDiv = document.createElement('div');
        bubbleDiv.className = 'message-bubble';

        const dotsDiv = document.createElement('div');
        dotsDiv.className = 'typing-dots';
        dotsDiv.innerHTML = '<span></span><span></span><span></span>';

        bubbleDiv.appendChild(dotsDiv);
        typingDiv.appendChild(bubbleDiv);

        this.chatMessages.appendChild(typingDiv);
        typingDiv.style.display = 'block';

        this.scrollToBottom();
    }

    hideTypingIndicator() {
        const typingIndicator = document.getElementById('typing-indicator');
        if (typingIndicator) {
            typingIndicator.remove();
        }
    }

    setLoading(loading) {
        this.sendButton.disabled = loading;
        this.messageInput.disabled = loading;

        if (loading) {
            this.sendButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        } else {
            this.sendButton.innerHTML = '<i class="fas fa-paper-plane"></i>';
        }
    }

    scrollToBottom() {
        this.chatMessages.scrollTop = this.chatMessages.scrollHeight;
    }
}

// Initialize chat app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new ChatApp();
});
