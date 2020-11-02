/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.controller;

import java.net.Socket;
import javax.swing.JFrame;
import consts.Consts;
import java.io.IOException;
import consts.Consts;
import model.SocketIO;

/**
 *
 * @author tienanh
 */
public class Client {
    private SocketIO socketIO;

    public Client(SocketIO socketIO) {
        this.socketIO = socketIO;
        
        // MainFrame
		JFrame mainFrame = new JFrame("Brick Breaker");
		mainFrame.setSize(Consts.SCREEN_WIDTH, Consts.SCREEN_HEIGHT);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
        
        // GamePlay
		GamePlay gamePlay = new GamePlay(socketIO);
		mainFrame.add(gamePlay);
		gamePlay.requestFocus();
		gamePlay.play();
    }
}
