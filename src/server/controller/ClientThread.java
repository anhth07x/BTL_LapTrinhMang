/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.controller;

import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.ClientState;
import model.Room;
import consts.Consts;
import java.awt.Color;
import java.io.Serializable;
import model.SocketIO;
import util.Utils;

/**
 *
 * @author tienanh
 */
public class ClientThread extends Thread implements Serializable{
	private ClientState state;
	private SocketIO socketIO;
	
	private Room selectedRoom;
	private WaitingRoomThread selectedRoomThread;
    private ArrayList<Room> listRoom;
    private ArrayList<WaitingRoomThread> listRoomThread;
	
	
	public ClientThread(ArrayList<Room> listRoom, 
                        ArrayList<WaitingRoomThread> listRoomThread,
                        Socket socket) {
		try {
            this.listRoom = listRoom;
            this.listRoomThread = listRoomThread;
            
            this.socketIO = new SocketIO();
			this.socketIO.setSocket(socket);
			this.socketIO.setInput(new ObjectInputStream(socket.getInputStream()));
            this.socketIO.setOutput(new ObjectOutputStream(socket.getOutputStream()));
            
            state = new ClientState();
			
		} catch (IOException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	@Override
	public void run() {
		while(!state.isSocketClose){
            try {
                Integer actionCode = (Integer)socketIO.getInput().readObject();
                System.out.println(actionCode);
                switch(actionCode){
                    case Consts.GET_LIST_ROOM:
                        this.getListRoom();
                        break;
                    case Consts.CREATE_ROOM_CODE:
                        this.createNewRoom();
                        break;
                    case Consts.BAR_MOVE:
                        this.handleBarMove();
                        break;
                    case Consts.JOIN_ROOM:
                        this.joinRoom();
                        break;
                    case Consts.UPDATE_WAITING_ROOM:
                        this.updateWaitingRoom();
                        break;
                    case Consts.START_GAME:
                        this.startGame();
                        break;
					case Consts.SEND_MESSAGE:
                        this.sendMessage();
                        break;
					case Consts.UPDATE_P1_BALL_COLOR:
						handleUpdateP1BallColor();
						break;
					case Consts.UPDATE_P2_BALL_COLOR:
						handleUpdateP2BallColor();
						break;
					case Consts.REMOVE_ROOM:
						handleRemoveRoom();
						break;
                }
            } catch (IOException ex) {
                System.out.println("Socket [ "+ Thread.currentThread().getName() +" ] Closed");
                this.state.isSocketClose = true;
				removePlayerFromRoom();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
	}
	
	public void handleRemoveRoom () {
		if (selectedRoomThread.getP2() != null){
			try {
				selectedRoomThread.getP2().getSocketIO().getOutput().writeObject(Consts.REMOVE_ROOM);
			} catch (IOException ex) {
				Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		// remove room
		listRoom.remove(getRoomIndexByName(selectedRoomThread.getRoom().getName()));
		listRoomThread.remove(getRoomThreadIndexByRoomName(selectedRoomThread.getRoom().getName()));
	}
	
	public void removePlayerFromRoom () {
		try {
			for (WaitingRoomThread roomThread : listRoomThread){
				// If player is room host => remove room, kick p2 
				if (roomThread.getRoom().getP1().getName() == state.getName()){
					if (roomThread.getRoom().getP2() != null){
						roomThread.getP2().getSocketIO().getOutput().writeObject(Consts.REMOVE_ROOM);
					}
					// remove room
					listRoom.remove(getRoomIndexByName(roomThread.getRoom().getName()));
					listRoomThread.remove(getRoomThreadIndexByRoomName(roomThread.getRoom().getName()));
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
    
	private void handleUpdateP1BallColor () {
		try {
			String color = (String) socketIO.getInput().readObject();
			selectedRoomThread.getP2().getSocketIO().getOutput().writeObject(Consts.UPDATE_P1_BALL_COLOR);
			selectedRoomThread.getP2().getSocketIO().getOutput().writeObject(color);
		} catch (IOException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private void handleUpdateP2BallColor () {
		try {
			String color = (String) socketIO.getInput().readObject();
			selectedRoomThread.getP1().getSocketIO().getOutput().writeObject(Consts.UPDATE_P2_BALL_COLOR);
			selectedRoomThread.getP1().getSocketIO().getOutput().writeObject(color);
		} catch (IOException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private void sendMessage() {
		try {
			String message = (String) socketIO.getInput().readObject();
			message = state.getName() + ": " + message + "\n";
			
			// Send flag
            selectedRoomThread.getP1().getSocketIO().getOutput().writeObject(Consts.SEND_MESSAGE);
            selectedRoomThread.getP2().getSocketIO().getOutput().writeObject(Consts.SEND_MESSAGE);
			
			// Send data
            selectedRoomThread.getP1().getSocketIO().getOutput().writeObject(message);
            selectedRoomThread.getP2().getSocketIO().getOutput().writeObject(message);
		} catch (IOException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
    private void startGame() {
        try {
            Room newRoom = (Room) socketIO.getInput().readObject();
            
            selectedRoomThread = this.getWaitingRoomThreadByRoomName(newRoom.getName());
			selectedRoomThread.getP1().setClientState(newRoom.getP1());
			selectedRoomThread.getP2().setClientState(newRoom.getP2());
            
			GamePlayThread gamePlay = new GamePlayThread();
            gamePlay.addPlayterToRoom(selectedRoomThread.getP1());
            gamePlay.addPlayterToRoom(selectedRoomThread.getP2());
			gamePlay.setSpeed(newRoom.getSpeed());
            gamePlay.setMap(selectedRoomThread.getRoom().getMap());
            gamePlay.start(); // Active Thread
            
            selectedRoomThread.getP1().getSocketIO().getOutput().writeObject(Consts.START_GAME);
            selectedRoomThread.getP2().getSocketIO().getOutput().writeObject(Consts.START_GAME);
            gamePlay.startGame();
			
			// remove room
			listRoom.remove(getRoomIndexByName(selectedRoomThread.getRoom().getName()));
            listRoomThread.remove(getRoomThreadIndexByRoomName(selectedRoomThread.getRoom().getName()));
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateWaitingRoom() {
        try {
            // Update Room To Host
            Room newRoom = (Room) socketIO.getInput().readObject();
            socketIO.getOutput().writeObject(newRoom);
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void getListRoom() {
        try {
            socketIO.getOutput().writeObject(listRoom);
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

	public void handleBarMove(){
        try {
            Integer keycode = (Integer)socketIO.getInput().readObject();
            switch (keycode) {
                case KeyEvent.VK_LEFT:
                    this.getClientState().getBar().setX(
                        this.getClientState().getBar().getX() >
                            this.getClientState().getBar().getSpeed() ?
                            this.getClientState().getBar().getX() -
                                this.getClientState().getBar().getSpeed() : 0
                    );
                    break;
                case KeyEvent.VK_RIGHT:
                    this.getClientState().getBar().setX(
                        this.getClientState().getBar().getX() +
                            this.getClientState().getBar().getWidth() <
                            consts.Consts.SCREEN_WIDTH*3/4 -
                                this.getClientState().getBar().getSpeed() ?
                            this.getClientState().getBar().getX() +
                                this.getClientState().getBar().getSpeed() :
                            consts.Consts.SCREEN_WIDTH*3/4 -
                                this.getClientState().getBar().getWidth()
                    );
                    break;
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
	}

    private void createNewRoom() {
        try {
            Room newRoom = (Room)socketIO.getInput().readObject();
            // Check room_name is unique
            for (Room tmp : listRoom){
                if (newRoom.getName().trim().toLowerCase()
                    .equals(tmp.getName().trim().toLowerCase())){
                    socketIO.getOutput().writeObject(false);
                    return;
                }
            }
            
            // Success
            socketIO.getOutput().writeObject(true);
            newRoom.setStatus(Consts.WAITING);
            listRoom.add(newRoom);
            
            // Create new waitingRoomThread
            selectedRoomThread = new WaitingRoomThread();
            selectedRoomThread.setRoom(newRoom);
            selectedRoomThread.setP1(this);
			selectedRoomThread.getP1().setClientState(selectedRoomThread.getRoom().getP1());
            listRoomThread.add(selectedRoomThread);
			
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
   
    private void joinRoom() {
        try {
            Room selectedRoom = (Room) socketIO.getInput().readObject();
			
            ClientState p2 = (ClientState) socketIO.getInput().readObject();
            for (Room room: listRoom){
                if (selectedRoom.getName().trim().toLowerCase().equals(room.getName().trim().toLowerCase())){
                    // update room
                    room.setP2(p2);
                    room.setStatus(Consts.READY);
                    // Send updated Room to sender
					socketIO.getOutput().reset();
                    socketIO.getOutput().writeObject(room);
                    
                    // Add to WaitingRoomThread
                    selectedRoomThread = this.getWaitingRoomThreadByRoomName(room.getName());
                    selectedRoomThread.setRoom(room);
                    selectedRoomThread.setP2(this);
					selectedRoomThread.getP2().setClientState(selectedRoomThread.getRoom().getP2());
					selectedRoomThread.getP2().socketIO.getOutput().writeObject(Consts.ROOM_EXISTS);
                    
                    // Send update action
                    selectedRoomThread.getP1().socketIO.getOutput().writeObject(Consts.UPDATE_WAITING_ROOM);
                    return;
                }
            }
			
			// If Room notexists
			socketIO.getOutput().writeObject(selectedRoom);
			socketIO.getOutput().writeObject(Consts.ROOM_NOT_EXISTS);
        } catch (IOException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    

    private WaitingRoomThread getWaitingRoomThreadByRoomName (String roomName) {
        for (WaitingRoomThread tmp : listRoomThread){
            if (tmp.getRoom().getName().trim().equals(roomName)){
                return tmp;
            }
        }
        return null;
    }
	
	private int getRoomThreadIndexByRoomName (String roomName) {
		int i = 0;
        for (WaitingRoomThread tmp : listRoomThread){
            if (tmp.getRoom().getName().trim().equals(roomName)){
                return i;
            }
			i++;
        }
        return -1;
    }

	private int getRoomIndexByName (String roomName) {
		int i = 0;
        for (Room tmp : listRoom){
            if (tmp.getName().trim().equals(roomName)){
                return i;
            }
			i++;
        }
        return -1;
    }
    
	public void setClientState(ClientState state) {
		this.state = state;
	}

	public ClientState getClientState() {
		return state;
	}

    public SocketIO getSocketIO() {
        return socketIO;
    }

    public void setSocketIO(SocketIO socketIO) {
        this.socketIO = socketIO;
    }
}
