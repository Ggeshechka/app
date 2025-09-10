# main.py

import tkinter as tk
from tkinter import messagebox

def on_button_click():
    messagebox.showinfo("Привет!", "Nuitka работает отлично! 🚀")

# Создаём окно
root = tk.Tk()
root.title("Моё Nuitka приложение")
root.geometry("300x150")

# Добавляем текст и кнопку
label = tk.Label(root, text="Нажми кнопку ниже:", font=("Arial", 12))
label.pack(pady=20)

button = tk.Button(root, text="Нажми меня!", command=on_button_click, font=("Arial", 12))
button.pack(pady=10)

# Запускаем главный цикл
root.mainloop()
