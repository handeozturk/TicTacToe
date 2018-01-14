package game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TicTacToe implements Runnable{
	
	private String  _ip = "localhost";
	private int _port = 22222;
	private Scanner _scanner = new Scanner(System.in);
	private JFrame _frame;
	private final int WIDTH = 506;
	private final int HEIGHT = 527;
	private Thread thread;
	
	private Painter _painter;
	private Socket _socket;
	private DataOutputStream dataOS;
	private DataInputStream dataIS;
	
	private ServerSocket serverSocket;
	
	private BufferedImage _board;
	private BufferedImage _redX;
	private BufferedImage _blueX;
	private BufferedImage _redCircle;
	private BufferedImage _blueCircle;
	
	private String[] spaces = new String[9];
	
	private boolean _yourTurn = false;
	private boolean circle = true;
	private boolean _accepted = false;
	private boolean _unableToCommunicate = false;
	private boolean _won = false;
	private boolean _enemyWon = false;
	private boolean _tie = false;
	
	private int lengthOfSpace = 160;
	private int errors = 0;
	private int firstSpot = -1;
	private int secondSpot = -1;
	
	private Font font = new Font("Verdana", Font.BOLD, 32);
	private Font smallerFont = new Font("Verdana", Font.BOLD, 20);
	private Font largerFont = new Font("Verdana", Font.BOLD, 50);

	private String waitingMsg = "Waiting for another player";
	private String unableToCommunicateMsg  = "Unable to communicate with another player";
	private String wonMsg = "You won!";
	private String enemyWonMsg = "Opponent Won!";
	private String tieMsg = "Game ended in a tie.";
	
			
	private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };
	
	public TicTacToe(){
		System.out.println("Please input the IP : ");
		_ip  = _scanner.nextLine();
		System.out.println("Please input the port : ");
		_port = _scanner.nextInt();
		while(_port < 1 || _port > 65535){
			System.out.println("The port you enterd was invalid. Please input another port");
			_port = _scanner.nextInt();
		}
		
		loadImages();
		
		_painter = new Painter();
		_painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		
		if(!connect())
			initializeServer();
		
		_frame = new JFrame();
		_frame.setTitle("Tic Tac Toe Game");
		_frame.setContentPane(_painter);
		_frame.setSize(WIDTH, HEIGHT);
		_frame.setLocationRelativeTo(null);
		_frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		_frame.setResizable(false);
		_frame.setVisible(true);
		
		thread = new Thread(this, "TicTacToe");
		thread.start();

	}
	
	public void run(){
		while(true)
		{
			tick();
			_painter.repaint();
			
			if(!circle && !_accepted){
				listenServerRequest();
			}
		}
	}
	
	private void render(Graphics g)
	{
		g.drawImage(_board, 0, 0, null);
		
		if(_unableToCommunicate){
			g.setColor(Color.RED);
			g.setFont(smallerFont);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(unableToCommunicateMsg);
			g.drawString(unableToCommunicateMsg, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
			return;
		}
		
		if(_accepted)
		{
			for(int i = 0; i < spaces.length; i++)
			{
				if(spaces[i] != null){
					
					if (spaces[i].equals("X")) {
						
						if(circle)
						{
							g.drawImage(_redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						}
						
						else
						{
							g.drawImage(_blueX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						}
					}
					else if (spaces[i].equals("O")) 
					{
						if (circle) 
						{
							g.drawImage(_blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						} 
						
						else 
						{
							g.drawImage(_redCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						}
					}
				}					
			}
			
			if (_won || _enemyWon)
			{
				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(10));
				g.setColor(Color.BLACK);
				g.drawLine(firstSpot % 3 * lengthOfSpace + 10 * firstSpot % 3 + lengthOfSpace / 2, (int) (firstSpot / 3) * lengthOfSpace + 10 * (int) (firstSpot / 3) + lengthOfSpace / 2, secondSpot % 3 * lengthOfSpace + 10 * secondSpot % 3 + lengthOfSpace / 2, (int) (secondSpot / 3) * lengthOfSpace + 10 * (int) (secondSpot / 3) + lengthOfSpace / 2);
				
				g.setColor(Color.RED);
				g.setFont(largerFont);
				
				if(_won)
				{
					int stringWidth = g2.getFontMetrics().stringWidth(wonMsg);
					g.drawString(wonMsg, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);	
					
					Lock lock = new ReentrantLock();
					 
					lock.lock();
					
					addDatabase();
					
					if(circle){
						connectMySql("Player2");
					}
					else
					{
						connectMySql("Player1");
					}
					
					lock.unlock();
				}
				else if (_enemyWon)
				{
					int stringWidth = g2.getFontMetrics().stringWidth(enemyWonMsg);
					g.drawString(enemyWonMsg, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
				}
				
			}
			
			if(_tie)
			{
				Graphics2D g2 = (Graphics2D) g;
				g.setColor(Color.BLACK);
				g.setFont(largerFont);
				int stringWidth = g2.getFontMetrics().stringWidth(tieMsg);
				g.drawString(tieMsg, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
			}
		}
		
		
		else
		{
			g.setColor(Color.RED);
			g.setFont(font);
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			int stringWidth = g2.getFontMetrics().stringWidth(waitingMsg);
			g.drawString(waitingMsg, WIDTH / 2 - stringWidth / 2, HEIGHT / 2);
		}
		
	}
	
	private void tick()
	{
		if(errors >= 10) 
			_unableToCommunicate = true;
		
		if(!_yourTurn && !_unableToCommunicate)
		{
			try
			{
				int space = dataIS.readInt();
				
				if(circle)
					spaces[space] = "X";
				else
					spaces[space] = "O";
				checkEnemyWin();
				checkTie();
				_yourTurn = true;
			}
			catch(IOException e)
			{
				e.printStackTrace();
				errors++;
			}
		}
	}
	

	
	private void checkWin()
	{
			
		for(int i = 0; i < wins.length; i++)
		{
			if(circle)
			{
				if(spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O")
				{
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					_won = true;
									
				}
								
			}
			else
			{				
				if(spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X")
				{
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					_won = true;
										
				}
				
			}
		}
		
	}
	
	private void checkEnemyWin()
	{		
		for(int i = 0; i < wins.length; i++)
		{
			if(circle)
			{
				if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") 
				{
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					_enemyWon = true;
					
				}
			}
				
			else 
			{
				if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") 
				{
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					_enemyWon = true;
					
				}
			}
			
		}
		
	}
	
	private void checkTie()
	{
		for(int i = 0; i < spaces.length; i++)
		{
			if(spaces[i] == null)
			{
				return;
			}
		}
		
		_tie = true;
	}
	
	public void addDatabase()
	{
		String url = "jdbc:mysql://localhost:3306/TicTacToe";
		String user = "root";
		String password = "admin";
		
		try{
			Connection conn = DriverManager.getConnection(url, user, password);
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery("select * from playerInfo");
			if (rs.next() == false) {
				String sql = "insert into playerInfo values (null, 'Player1', 0); ";
				stmnt.executeUpdate(sql);
				String sql2 = "insert into playerInfo values (null, 'Player2', 0); ";
				stmnt.executeUpdate(sql2);
				System.out.println("Player informations recorded to database");
			}
		}
		catch(Exception exc)
		{
			exc.printStackTrace();
		}
	}

	public void connectMySql(String _playerName)
	{
		String url = "jdbc:mysql://localhost:3306/TicTacToe";
		String user = "root";
		String password = "admin";
		
		try{
			Connection conn = DriverManager.getConnection(url, user, password);
			Statement stmnt = conn.createStatement();
			String sql = "update playerInfo "
					   + "set totalWinning = totalWinning + 1 "
					   + "where playerName = '"+_playerName+"'";  
			stmnt.executeUpdate(sql);
			String sql2 = "insert into gameInfo (playerID, winner) " 
					   +"select playerID, playerName from playerinfo "
					   + "where playerName = '"+_playerName+"'";
			stmnt.executeUpdate(sql2);
			System.out.println("Game informations recorded to database");
		}
		catch(Exception exc)
		{
			exc.printStackTrace();
		}
	}
	
	public void listenServerRequest(){
		Socket socket = null;
		try{
			socket = serverSocket.accept();
			dataOS = new DataOutputStream(socket.getOutputStream());
			dataIS = new DataInputStream(socket.getInputStream());
			_accepted = true;
			System.out.println("CLIENT HAS REQUESTED TO JOIN");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean connect()
	{
		try
		{
			_socket = new Socket(_ip, _port);
			dataOS = new DataOutputStream(_socket.getOutputStream());
			dataIS = new DataInputStream(_socket.getInputStream());
			_accepted = true;
		}
		catch(IOException e)
		{
			System.out.println("Unable to connect to the address: " + _ip + ":" + _port + " | Starting a server");
			return false;
		}
		
		System.out.println("Successfully connected to the server.");
		return true;
	}
	
	private void initializeServer()
	{
		try
		{
			serverSocket = new ServerSocket(_port, 8, InetAddress.getByName(_ip));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		_yourTurn = true;
		circle = false;
	}
	
	private void loadImages()
	{
		try{
			_board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
			_redX = ImageIO.read(getClass().getResourceAsStream("/redX.png"));
			_blueX = ImageIO.read(getClass().getResourceAsStream("/blueX.png"));
			_blueCircle = ImageIO.read(getClass().getResourceAsStream("/blueCircle.png"));
			_redCircle = ImageIO.read(getClass().getResourceAsStream("/redCircle.png"));
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args){
		TicTacToe ttt = new TicTacToe();
	}
	
	public class Painter extends JPanel implements MouseListener{

		private static final long serialVersionUID = 1L;
		
		public Painter(){
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}
		
		@Override
		public void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			if(_accepted)
			{
				if(_yourTurn && !_unableToCommunicate && !_won && !_enemyWon)
				{
					int x = e.getX() / lengthOfSpace;
					int y = e.getY() / lengthOfSpace;
					y *= 3;
					int position = x + y;
					
					if(spaces[position] == null)
					{
						if(!circle)
							spaces[position] = "X";
						else
							spaces[position] = "O";
						
						_yourTurn = false;
						repaint();
						Toolkit.getDefaultToolkit().sync();
						
						try
						{
							dataOS.writeInt(position);
							dataOS.flush();
						}
						catch(IOException e1)
						{
							e1.printStackTrace();
							errors++;
						}
						
						System.out.println("DATA WAS SENT");
						checkWin();
						checkTie();
																		
					}
				}
							
				
			}
			
		}

		@Override
		public void mouseEntered(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent arg0) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
