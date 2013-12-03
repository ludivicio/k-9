package org.ancode.secmail.controller;

import org.ancode.secmail.mail.Message;

public interface MessageRemovalListener {
    public void messageRemoved(Message message);
}
