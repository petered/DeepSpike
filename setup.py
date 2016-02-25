
from setuptools import setup

import imp

THIS_DIR = os.path.dirname(os.path.realpath(__file__))

foo = imp.load_source('deepstream', THIS_DIR+'/src/main/python/deepstream/deepstream_python_directory.py')
foo.MyClass()


setup(name='DeepStream',
      author='Peter',
      author_email='poconn4@gmail.com',
      url='https://github.com/petered/deepstream',
      long_description='A Java library for Spiking Deep Neural Networks',
      version=0,
      packages=[],
      scripts=[])
