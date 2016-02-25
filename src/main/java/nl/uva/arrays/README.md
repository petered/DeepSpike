# Arrays

This package was made to facilitate array operations for DeepStream.

The main problem that we're trying to solve in this package is that we want the
efficiency of primative arrays, and the ability to change the underlying data
type with a parameter.  This is not normally trivial because Java doesn't allow
primatives to be used as generics.

We use the same naming conventions as the Nd4j project.  The differences between
this package and Nd4j are:
1) We create different classes for different array types and dimensions (e.g.
   we have a DoubleVector, DoubleMatrix, FloatVector instead of everything being
   an INDArray.
2) Backend, for now, is native Java.  For whatever reason this seems to be working
   25x faster than Nd4j, but this could be some kind of BLAS issue.
