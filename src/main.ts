import { invoke } from "@tauri-apps/api/core";

async function executeAction(action: string, data: string = ""): Promise<string> {
  const res = await invoke<{ value: string }>('plugin:xray|ping', {
    payload: {
      value: JSON.stringify({ action, data })
    }
  });
  return res.value;
}

const pingButton = document.getElementById('ping-btn');
const resultSpan = document.getElementById('result');

pingButton?.addEventListener('click', async () => {
  if (resultSpan) resultSpan.textContent = "Ожидание...";
  try {
    const result = await executeAction("startVpn", "{ \"server\": \"192.168.1.1\" }");
    if (resultSpan) {
      resultSpan.textContent = result;
    }
  } catch (error) {
    if (resultSpan) resultSpan.textContent = `Ошибка: ${error}`;
    console.error('Ошибка:', error);
  }
});

// Код для логов
const logsButton = document.getElementById('logs-btn');
const logsOutput = document.getElementById('logs-output');

async function fetchLogs() {
  if (logsOutput && logsOutput.textContent === "") {
    logsOutput.textContent = "Загрузка логов...";
  }
  try {
    const logs = await executeAction("getLogs");
    if (logsOutput) {
      logsOutput.textContent = logs;
      logsOutput.scrollTop = logsOutput.scrollHeight;
    }
  } catch (error) {
    if (logsOutput) {
      logsOutput.textContent = `Ошибка загрузки логов: ${error}`;
    }
  }
}

logsButton?.addEventListener('click', fetchLogs);

// Автообновление логов (раскомментируй, если нужно)
// setInterval(fetchLogs,
2000);
