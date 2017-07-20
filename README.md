This is my codebase for the Credit Suisse interview test "Live Order Board".

This document describes the design choices made.

0) Architecture:
Looking at the problem spec, it appears obvious that the main issue is consistency of the data under parallel execution
of the order operations. To simulate this realistically, I implemented a simple REST api app to post and delete orders
as well as retrieve the board.
In order to guarantee scalability and isolation, I have used the Akka framework, in particular the Distributed Data and
Cluster Sharding extensions.
Cluster Sharding allows us to keep all data in memory (in real world it would need to be backed by persistence) as well
as easily scaling out if needed.
Distributed Data allows us to keep track of the data in the board from multiple sources in a thread-safe way.

Each order is modeled as an individual actor which, upon creation or deletion, communicates to the board the variation
of the quantity traded at that price for that order type. The DData extensions keep track of this information using a
Map from price -> quantity, implemented as a PNCounter (Positive/Negative).

The board is then retrieved upon request and "prettified" by sorting correctly the data and removing 0 entries.

1) Data Format:
Due to the decimal nature of the values, the API accepts quantities and prices as Integers (grams and pence, respectively)
This is done to avoid rounding errors.

2) Missing parts to be a "real" system:
There are a few key simplification and assumptions in place here for the sake of keeping the codebase small and
manageable to review for the scope of an interview test. Here are a few things that have been left out and should instead
be there in a real system:

- There is no backing persistence here, all is in memory and all is lost upon shutdown.
- There is no check on the validity of the prices (negatives, 0's, etc), since no further specification for those was provided:
It should probably check something
- There is no purging mechanism in place for 0 quantities (order create + delete at the same price): while these are not returned,
in a real system they should also be purged on a schedule, but I did not want to introduce further unnecessary complexity
- The tests are modeled strictly on the specifications. One nice set of tests that one might want to add in a real system
concerns non-functional requirements such as load stressing and security.
- No configuration-ability is provided (things like http and cluster port etc.). A real system would parametrize those.
- Orders have an additional (with respect to the specification) field, orderId, an UUID to use in the delete operation. This
is generated from the application. In a real world scenario, probably stronger checks on its unicity should be performed.

