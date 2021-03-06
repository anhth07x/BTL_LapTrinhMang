/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.controller;

import client.view.Game;
import client.view.LAN;
import consts.Consts;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import model.Brick;
import model.ClientState;
import model.EnhanceItem;
import model.MapState;
import model.SocketIO;

/**
 *
 * @author tienanh
 */
public class GamePlay extends JPanel{
	// Gameplay state
    private int width, height, padding = 50;
	private boolean isHost;
	private boolean isPlay;
	private boolean inProgress;
	private int gameMode;
	
	private boolean isPause;
	private boolean isShowCounter;
	private Integer counter = 0;
	
	private boolean isGameLose, isGameWin;
	
	private boolean isEnemyDisconnected, isShow = false;
	private MapState mapState;
	private ClientState p1, p2;
    
    // In/Out
	private SocketIO socketIO;
	private ImageIcon disconnectedGif;
	
	public GamePlay(SocketIO socketIO, boolean isHost) {
        this.socketIO = socketIO;
		this.isHost = isHost;
		p1 = new ClientState();
		p2 = new ClientState();
        isEnemyDisconnected = false;
		inProgress = false;
		isPlay = true;
        initNewGame();
        initMedias();
        // handle Bar move
        addKeyListener(this.handleBarMove());
        addMouseListener(this.handleClickEvent());
	}
	
	public void initNewGame (){
		isPlay = isGameLose = isGameWin = isPause = false;
		isShowCounter = true;
		
		// Init Jpanel
		setSize(Consts.SCREEN_WIDTH, Consts.GAMPLAY_HEIGHT);
		setPreferredSize(new Dimension(Consts.SCREEN_WIDTH, Consts.GAMPLAY_HEIGHT));
		setBackground(Color.BLACK);
		setFocusable(true);
		setVisible(true);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		if (this.isPlay){
			super.paintComponent(g); 
			drawBasicState(g);
			drawRightMenu(g);
			drawGamePlayState(g);
			g.dispose();
		}
	}
	
	public void drawGamePlayState (Graphics g) {
		if (isGameLose){
			g.setColor(Color.RED);
			g.setFont(new Font("LifeCraft", Font.PLAIN, 50));
			g.drawString("GAME OVER",Consts.GAMPLAY_WIDTH / 2 - 50*3, Consts.GAMPLAY_HEIGHT / 2);
		}
		
		if (isGameWin){
			g.setColor(Color.GREEN);
			g.setFont(new Font("LifeCraft", Font.PLAIN, 50));
			g.drawString("GAME WIN",Consts.GAMPLAY_WIDTH / 2 - 50*3, Consts.GAMPLAY_HEIGHT / 2);
		}
		
		// Counter on first Init
		if (isShowCounter){
			g.setColor(Color.RED);
			g.setFont(new Font("LifeCraft", Font.PLAIN, 200));
			g.drawString(counter.toString(), Consts.GAMPLAY_WIDTH / 2 - 100, Consts.GAMPLAY_HEIGHT / 2);
		}
	}
	
	public void drawBasicState (Graphics g) {
		// Ball p1
		if (p1.getBall() != null){
			if (p1.getBall().isPowerBall()) g.setColor(Color.PINK);
			else g.setColor(p1.getBall().getColor());
			g.fillOval(p1.getBall().getX(), p1.getBall().getY(), p1.getBall().getRadius(), p1.getBall().getRadius());
		}
		
		// Bar p1
		if (gameMode == Consts.ONE_BALL) g.setColor(Color.BLUE);
		else g.setColor(Color.GREEN);
		g.fillRect(p1.getBar().getX(), p1.getBar().getY(), p1.getBar().getWidth(), p1.getBar().getHeight());	
		// Draw Counter for enhance item if exsit
		float remainingPercentP1 = p1.getEnhanceItems() != null && p1.getEnhanceItems().size() > 0 ? p1.getEnhanceItems().get(0).getRemainingTime() / (float)Consts.ENHANCE_ITEM_LAST : 0;
		g.setColor(Color.ORANGE);
		g.fillRect(
			p1.getBar().getX() + 1, 
			p1.getBar().getY() + p1.getBar().getHeight(), 
			(int)(p1.getBar().getWidth() * remainingPercentP1),
			5);
		
		// Draw brick
		for (int i = 0 ; i < this.mapState.getRow() ; i++) {
			for (int j = 0; j < this.mapState.getCol(); j++){
				Brick curBrick = this.mapState.getBricks()[i*this.mapState.getCol()+ j];
				if ( curBrick.getIsDisplay()){
					if (curBrick.getType() != Consts.NORMAL) g.setColor(Color.RED); // EnhanceItem
					else g.setColor(Color.YELLOW);
					g.fillRect(curBrick.getX(), curBrick.getY(), Consts.BRICK_WIDTH, Consts.BRICK_HEIGHT);
				}
			}
		}
		
		
		// Ball p2
		if (p2.getBall() != null){
			if (p2.getBall().isPowerBall()) g.setColor(Color.PINK);
			else g.setColor(p2.getBall().getColor());
			g.fillOval(p2.getBall().getX(), p2.getBall().getY(), p2.getBall().getRadius(), p2.getBall().getRadius());
		}
	
		// Bar p2	
		g.setColor(Color.GREEN);
		g.fillRect(p2.getBar().getX(), p2.getBar().getY(), p2.getBar().getWidth(), p2.getBar().getHeight());
		// Draw Counter for enhance item if exsit 
		g.setColor(Color.ORANGE);
		float remainingPercent = p2.getEnhanceItems() != null && p2.getEnhanceItems().size() > 0 ? p2.getEnhanceItems().get(0).getRemainingTime() / (float)Consts.ENHANCE_ITEM_LAST : 0;
		g.fillRect(
			p2.getBar().getX(), 
			p2.getBar().getY() - 5, 
			(int)(p2.getBar().getWidth() * remainingPercent),
			5);
		// Draw brick
		for (int i = 0 ; i < this.mapState.getRow() ; i++) {
			for (int j = 0; j < this.mapState.getCol(); j++){
				Brick curBrick = this.mapState.getBricks()[i*this.mapState.getCol()+ j];
				if ( curBrick.getIsDisplay()){
					if ( curBrick.getType() != Consts.NORMAL) g.setColor(Color.RED);
					else g.setColor(Color.YELLOW);
					g.fillRect(curBrick.getX(), curBrick.getY(), Consts.BRICK_WIDTH, Consts.BRICK_HEIGHT);
				}
			}
		}
		
		// Draw Enhance Item
		for (EnhanceItem item : mapState.getEnhanceItems()){
			item.getIcon().paintIcon(this, g, item.getX(), item.getY());
		}
	}
	
	public void drawRightMenu (Graphics g) {
		// Line separate 
		g.setColor(Color.WHITE);
		g.drawLine(Consts.GAMPLAY_WIDTH, 0, Consts.GAMPLAY_WIDTH, Consts.GAMPLAY_HEIGHT);
			
		// Menu
		// Point p2
		g.setColor(Color.RED);
		g.setFont(new Font("LifeCraft", Font.PLAIN, 20));
		g.drawString(p2.getName() + ": " + p2.getPoint(), Consts.GAMPLAY_WIDTH + 50, 50);
		
		// determine "your side"
		if (!isHost){
			g.setColor(Color.YELLOW);
			drawCircleWithX(g, Consts.GAMPLAY_WIDTH + 20, 50 - 15, 15);
		}
		
		// Disconnected
		if (p2.isSocketClose){
			disconnectedGif.paintIcon(this, g, Consts.GAMPLAY_WIDTH + 50, 80);
			g.setColor(Color.YELLOW);
			g.drawString("Disconnected", Consts.GAMPLAY_WIDTH + 110, 110);
		}
			
		// Line separate
		g.setColor(Color.WHITE);
		g.drawLine(Consts.GAMPLAY_WIDTH, Consts.GAMPLAY_HEIGHT / 2, Consts.SCREEN_WIDTH, Consts.GAMPLAY_HEIGHT / 2);

		// Point p1
		g.setColor(Color.RED);
		g.setFont(new Font("LifeCraft", Font.PLAIN, 20));
		g.drawString(p1.getName() + ": " + p1.getPoint(), Consts.GAMPLAY_WIDTH + 50, Consts.GAMPLAY_HEIGHT / 2 + 50);
		
		// determine "your side"
		if (isHost){
			g.setColor(Color.YELLOW);
			drawCircleWithX(g, Consts.GAMPLAY_WIDTH + 20, Consts.GAMPLAY_HEIGHT / 2 + 50 - 15, 15);
		}
			
		// Disconnected
		if (p1.isSocketClose){
			disconnectedGif.paintIcon(this, g, Consts.GAMPLAY_WIDTH + 50,  Consts.GAMPLAY_HEIGHT / 2 + 80);
			g.setColor(Color.YELLOW);
			g.drawString("Disconnected", Consts.GAMPLAY_WIDTH + 110, Consts.GAMPLAY_HEIGHT / 2 + 110);
		}

        // Save BTN
		if (gameMode != Consts.ONE_BALL) {
			g.setColor(Color.RED);
			g.drawRect(Consts.GAMPLAY_WIDTH + 20 + 150  + 50, Consts.GAMPLAY_HEIGHT - 70 - 50 - 20, 150, 50);
			g.drawString("Save", Consts.GAMPLAY_WIDTH + 20 + 150  + 50 + 50, Consts.GAMPLAY_HEIGHT - 70 - 50 - 20 + 32);
		}
        
        
//		// Pause BTN 
//		g.setColor(Color.RED);
//		g.drawRect(Consts.GAMPLAY_WIDTH + 20, Consts.GAMPLAY_HEIGHT - 70, 150, 50);
//		g.drawString(isPause ? "Play" : "Pause", Consts.GAMPLAY_WIDTH + 20 + 45, Consts.GAMPLAY_HEIGHT - 70 + 32);

		// Exit BTN 
		g.setColor(Color.RED);
		g.drawRect(Consts.GAMPLAY_WIDTH + 20 + 150  + 50, Consts.GAMPLAY_HEIGHT - 70, 150, 50);
		g.drawString("Exit", Consts.GAMPLAY_WIDTH + 20 + 150  + 50 + 50, Consts.GAMPLAY_HEIGHT - 70 + 32);
	}
	
	public void drawCircleWithX (Graphics g, int startX, int startY, int padding) {
		g.drawLine(startX, startY, startX + padding, startY + padding);
		g.drawLine(startX + padding, startY, startX , startY + padding);
		g.drawLine(startX + padding / 2, startY, startX + padding / 2, startY + padding);
		g.drawLine(startX, startY  + padding / 2, startX + padding, startY + padding / 2 );
	}
	
	// Listen on server send data
	public void play () {
		boolean isSocketClose = isHost ? p1.isSocketClose : p2.isSocketClose;
		while (!isSocketClose){
			try {
				Integer actionCode = (Integer) socketIO.getInput().readObject();
				switch (actionCode){
					case Consts.COUNTER_BEFORE_START:
						handleCounterBeforeStart();
						break;
					case Consts.UPDATE_GAMEPLAY_STATE:
						updateGamePlayState();
						break;
					case Consts.OTHER_PLAYER_LOST_CONNECTION:
						otherPlayerLostConnection();
						break;
					case Consts.GAME_LOSE:
						handleGameOver();
						break;
					case Consts.GAME_WIN:
						handleGameWin();
						break;
					case Consts.GAME_PAUSE:
						handleGamePause();
						break;
					case Consts.GAME_UNPAUSE:
						handleGameUnPause();
						break;
					case Consts.ONE_BALL:
						handleModeOneBall();
						break;
				}
			} catch (IOException ex) {
                System.out.println("Socket [ "+ Thread.currentThread().getName() +" ] Closed");
                if (isHost) this.p1.isSocketClose = true;
				else this.p2.isSocketClose = true;
				return;
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(GamePlay.class.getName()).log(Level.SEVERE, null, ex);
            }
		}
	}
	
	public void handleModeOneBall () {
		gameMode = Consts.ONE_BALL;
		customRepaint();
	}
	
	public void handleGamePause () {
		isPause = true;
		customRepaint();
	}
	
	public void handleGameUnPause () {
		isPause = false;
		customRepaint();
	}
	
	public void handleGameWin () {
		isGameWin = true;
		customRepaint();
	}
	
	public void handleGameOver () {
		isGameLose = true;
		customRepaint();
	}
	
	public void handleCounterBeforeStart(){
		try {
            isShowCounter = true;
			counter = (Integer) socketIO.getInput().readObject();
			if (counter == 0) isShowCounter = false;
            customRepaint();
		} catch (IOException ex) {
			Logger.getLogger(GamePlay.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(GamePlay.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public void otherPlayerLostConnection () {
		updateGamePlayState();
		isEnemyDisconnected = true;
		customRepaint();
	}
	
	public void customRepaint() {
		repaint();
		// For linux system.
		if ( System.getProperty("os.name").equals("Linux")){
				Toolkit.getDefaultToolkit().sync();
		}
	}
	
	public void updateGamePlayState () {
		try {
			p1 = (ClientState)socketIO.getInput().readObject();
			p2 = (ClientState)socketIO.getInput().readObject();
			mapState = (MapState)socketIO.getInput().readObject();
			isPlay = true;
			customRepaint();
		} catch (IOException ex) {
			Logger.getLogger(GamePlay.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(GamePlay.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private KeyAdapter handleBarMove() {
		return new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent ke) {
				try {
					ArrayList<Integer> validKeyCode  = new ArrayList<Integer>();
					validKeyCode.add(KeyEvent.VK_LEFT);
					validKeyCode.add(KeyEvent.VK_RIGHT);
					validKeyCode.add(KeyEvent.VK_ENTER);
					if (validKeyCode.contains(ke.getKeyCode())){
                        // Action Code
                        socketIO.getOutput().writeObject(Consts.BAR_MOVE);
                        // Send data
						socketIO.getOutput().writeObject(ke.getKeyCode());
					}
				} catch (Exception ex) {
					Logger.getLogger(GamePlay.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		};
	}

	private MouseListener handleClickEvent() {
		JPanel self = this;
		return new MouseAdapter() {
            @Override
			public void mouseClicked(MouseEvent me) {
				try {				
					if (!isShowCounter){
                        // Save BTN
                        if (me.getX() >= Consts.GAMPLAY_WIDTH + 220 && me.getX() <= Consts.GAMPLAY_WIDTH + 220 + 150 &&
							me.getY() >= Consts.GAMPLAY_HEIGHT - 140 && me.getY() <= Consts.GAMPLAY_HEIGHT - 140 + 50 && gameMode != Consts.ONE_BALL){
                            // Create File Name 
                            int index = 0;
                            String fileName = "src/data/save/save-" + index + ".txt";
                            File file = new File(fileName);
                            while(file.exists()) {
                                fileName = "src/data/save/save-" + ++index +".txt";
                                file = new File(fileName);
                            }

                            // Capture
                            BufferedImage img = new BufferedImage(Consts.GAMPLAY_WIDTH, Consts.GAMPLAY_HEIGHT, BufferedImage.TYPE_INT_RGB);
                            self.paint(img.getGraphics());
                            File outputfile = new File("src/data/save/image/preview-"+index+".png");
                            ImageIO.write(img, "png", outputfile);

                            socketIO.getOutput().writeObject(Consts.SAVE_GAME);
                            socketIO.getOutput().writeObject(fileName);                            
                        }
						// Pause BTN 
						// Disabled. Not fix yet
                        else if (false && me.getX() >= Consts.GAMPLAY_WIDTH + 20 && me.getX() <= Consts.GAMPLAY_WIDTH + 20 + 150 &&
							me.getY() >= Consts.GAMPLAY_HEIGHT - 70 && me.getY() <= Consts.GAMPLAY_HEIGHT - 70 + 50){
							if (!isPause) {
								isPause = true;
								socketIO.getOutput().writeObject(Consts.GAME_PAUSE);
							} else {
								socketIO.getOutput().writeObject(Consts.GAME_UNPAUSE);
							}
						// Exit BTN
						}else if (
							me.getX() >= Consts.GAMPLAY_WIDTH + 220 && me.getX() <= Consts.GAMPLAY_WIDTH + 220 + 150 &&
							me.getY() >= Consts.GAMPLAY_HEIGHT - 70 && me.getY() <= Consts.GAMPLAY_HEIGHT - 70 + 50) {
							JFrame parent = (JFrame)self.getTopLevelAncestor();
							parent.dispose();
							new LAN(socketIO, null, false).setVisible(true);
						}
					}
				} catch (IOException ex) {
					Logger.getLogger(GamePlay.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		};
	}

	private void initMedias() {
		disconnectedGif = new ImageIcon(getClass().getResource("/data/image/loading_50x50.gif"));
	}
}