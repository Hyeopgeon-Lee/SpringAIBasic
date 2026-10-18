#!/usr/bin/env python3
"""Train Logistic Regression on Iris and export ONNX.

Dependencies (only required to RE-TRAIN; not required to RUN the Java demo):
    python -m pip install scikit-learn numpy onnx

The repository already contains the exported model. The Java application only
needs Java 17 and Maven. Output: src/main/resources/models/iris-logreg.onnx
"""
from pathlib import Path

import numpy as np
import onnx
from onnx import TensorProto, helper
from sklearn.datasets import load_iris
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score
from sklearn.model_selection import train_test_split

X, y = load_iris(return_X_y=True)
X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)
model = LogisticRegression(max_iter=500, random_state=42)
model.fit(X_train, y_train)

print("held-out accuracy:", round(accuracy_score(y_test, model.predict(X_test)), 4))
print("species class order:", model.classes_)
# sklearn: coef_.shape = [classes, features].
# ONNX MatMul requires weights.shape = [features, classes].
weights = np.asarray(model.coef_.T, dtype=np.float32)
biases = np.asarray(model.intercept_, dtype=np.float32)

graph = helper.make_graph(
    nodes=[
        helper.make_node("MatMul", ["features", "weights"], ["linear"]),
        helper.make_node("Add", ["linear", "intercepts"], ["scores"]),
        helper.make_node("Softmax", ["scores"], ["probabilities"]),
    ],
    name="iris-logistic-regression",
    inputs=[helper.make_tensor_value_info("features", TensorProto.FLOAT, [1, 4])],
    outputs=[helper.make_tensor_value_info("probabilities", TensorProto.FLOAT, [1, 3])],
    initializer=[
        helper.make_tensor("weights", TensorProto.FLOAT, [4, 3], weights.ravel().tolist()),
        helper.make_tensor("intercepts", TensorProto.FLOAT, [3], biases.tolist()),
    ],
)
onnx_model = helper.make_model(
    graph, producer_name="SpringAIBasic",
    opset_imports=[helper.make_opsetid("", 13)],
)
# IR 8 works with both current ONNX Runtime and older tooling.
onnx_model.ir_version = 8
onnx.checker.check_model(onnx_model)
output = Path("src/main/resources/models/iris-logreg.onnx")
output.parent.mkdir(parents=True, exist_ok=True)
onnx.save(onnx_model, output)
print("saved:", output, "bytes:", output.stat().st_size)
