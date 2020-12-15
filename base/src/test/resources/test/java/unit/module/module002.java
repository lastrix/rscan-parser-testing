module example {
    exports example.a to a.b.c, d.e.f;
    opens example.a to a.b.c, d.e.f;
}
