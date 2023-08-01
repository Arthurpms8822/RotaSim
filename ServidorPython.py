# -*- coding: utf-8 -*-
"""
Created on Mon Jul 31 13:07:33 2023

@author: HP 840
"""

from flask import Flask, jsonify, request

app = Flask(_name_)

# To store the JSON data
data_list = []
time_data_list = []

@app.route('/')
def home():
    return jsonify(data_list)

@app.route('/send', methods=['POST'])
def send_json():
    data = request.get_json()
    data_list.append(data)
    return jsonify({'result': 'success'}), 200

@app.route('/sendNewTime', methods=['POST'])
def send_time_json():
    data = request.get_json()
    time_data_list.append(data)
    return jsonify({'result': 'success'}), 200

@app.route('/receive', methods=['GET'])
def receive_json():
    return jsonify(data_list[-1] if data_list else {}), 200

@app.route('/receiveNewTime', methods=['GET'])
def receive_time_json():
    return jsonify(time_data_list[-1] if time_data_list else {}), 200

if _name_ == "_main_":
    app.run(port=5000)