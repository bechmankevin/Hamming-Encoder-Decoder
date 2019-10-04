# Hamming-Encoder-Decoder

Include file to encode/decode in arguments; e.g.:
```
java Hamming data.txt
```

  The general purpose of this program is to simulate an environment where data is encoded, corrupted, then decoded using the
Hamming code (8,4) error correction technique.  Each byte that is decoded can have zero, one, or two bits flipped (randomly chosen).  In the case of one bit, the bit is detected and flipped back to its correct value.  In the case of
two bits, no error correction is performed and a message prints out in the console.
