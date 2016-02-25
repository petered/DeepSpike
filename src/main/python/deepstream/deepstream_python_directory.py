import os
__author__ = 'peter'

THIS_DIR = os.path.dirname(os.path.realpath(__file__))

def locate_deepstream_spiking_mlp():
    return os.path.join(THIS_DIR, 'target', 'cla')

