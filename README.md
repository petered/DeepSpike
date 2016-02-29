# DeepSpike

DeepSpike is a Java package to help you build deep spiking neural networks and spiking deep neural networks.  Currently, it includes a Spiking Multi-Layer Perceptron.

## Getting Started

In our experiments the code was called from Python, so currently all tests are in the Python code, which is in our [Spiking-MLP Repo](https://github.com/petered/spiking-mlp).  If you want to try DeepSpike, go to that repo and follow the installation instructions.

## About DeepSpike

DeepSpike is a library for building Event-Based neural networks.  "Event-Based" means that the units communicate by sending events to one another, and downstream units only update their state upon receiving events.  Contrast this to regular deep networks, where we think of each unit as a function that takes an input and produces an output.  In an event-based architecture, if, in processing an input event, a unit decides that it should produce an event, it announces "Hey, I am firing an event", and an event-handler decides what to do with it.  
