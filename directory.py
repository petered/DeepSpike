import os
__author__ = 'peter'

THIS_DIR = os.path.dirname(os.path.realpath(__file__))


def locate_class_dir():
    return os.path.abspath(os.path.join(THIS_DIR, 'target', 'classes'))
