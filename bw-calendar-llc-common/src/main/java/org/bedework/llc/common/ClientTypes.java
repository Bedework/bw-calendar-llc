package org.bedework.llc.common;

public interface ClientTypes {
  enum ClientType {
    // Unauthenticated
    guest,

    // Pretty much same as guest but it's the public client view
    publick,

    // Again much the same but a public feeder
    feeder,

    // public client requiring authentication. Acts like guest mostly
    publicAuth,

    // Submission client
    submission,

    // Personal/group scheduling
    personal,

    // Admin client
    admin
  }
}
