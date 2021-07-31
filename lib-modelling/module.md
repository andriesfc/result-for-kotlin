# Module ResultK Modelling

Opinionated extension the orginal library for modelling domain errors and API results

# Package resultk.modelling.error

A _**very**_ opinionated framework for modelling domain errors. Addresses the following concerns:

1. Modelling error codes from enums, numbers, text and sealed classes.
2. Ability to model internationalized error messages.
3. Ability to model developer friendly error messages.

# Package resultk.modelling.internal

Some internal functions and classes used by the top level packages. Not intended for public consumption.

# Package resultk.modelling.internal.templating

Various internal templating engines used to bind errors in order to produce messages passed to exceptions.