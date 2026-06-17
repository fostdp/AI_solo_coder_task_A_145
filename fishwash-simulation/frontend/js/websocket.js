class WebSocketClient {
    constructor(dashboard) {
        this.dashboard = dashboard;
        this.stompClient = null;
        this.isConnected = false;
    }

    connect() {
        const socket = new SockJS('http://localhost:8080/ws');
        this.stompClient = Stomp.over(socket);
        this.stompClient.connect({}, (frame) => {
            this.onConnected(frame);
        }, (error) => {
            this.onError(error);
        });
    }

    onConnected(frame) {
        this.isConnected = true;
        console.log('WebSocket connected:', frame);
        this.updateConnectionStatus(true);
        this.subscribeAlerts();
    }

    onError(error) {
        this.isConnected = false;
        console.error('WebSocket error:', error);
        this.updateConnectionStatus(false);
    }

    subscribeAlerts() {
        if (!this.stompClient || !this.isConnected) return;
        this.stompClient.subscribe('/topic/alerts', (message) => {
            try {
                const alert = JSON.parse(message.body);
                if (this.dashboard) {
                    this.dashboard.showAlert(alert);
                }
            } catch (e) {
                console.error('Failed to parse alert message:', e);
            }
        });
    }

    disconnect() {
        if (this.stompClient) {
            this.stompClient.disconnect(() => {
                this.isConnected = false;
                this.updateConnectionStatus(false);
                console.log('WebSocket disconnected');
            });
        }
    }

    updateConnectionStatus(connected) {
        const statusEl = document.getElementById('connectionStatus');
        if (statusEl) {
            if (connected) {
                statusEl.classList.add('connected');
                statusEl.querySelector('span:last-child').textContent = '已连接';
            } else {
                statusEl.classList.remove('connected');
                statusEl.querySelector('span:last-child').textContent = '未连接';
            }
        }
    }
}
