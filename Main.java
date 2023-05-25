import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.image.*;

import javax.swing.JFrame;

public class Main extends Canvas{
	static int stage = -1;
	static int selected;
	static int progress;
	static int found = 0;
	static int dasKey;
	static int rotation;
	static int showSolution = -1;
	static int hold = 0;
	static long dasTime;
	static long start;
	static long solution;
	static long disp;
	static boolean update = true;
	static boolean canHold = true;
	static Point[][] shapes = {{}, //I J L O S T Z
			{new Point(-1, 0), new Point(0, 0), new Point(1, 0), new Point(2, 0)},
			{new Point(-1, -1), new Point(-1, 0), new Point(0, 0), new Point(1, 0)},
			{new Point(-1, 0), new Point(0, 0), new Point(1, 0), new Point(1, -1)},
			{new Point(0, -1), new Point(0, 0), new Point(1, 0), new Point(1, -1)},
			{new Point(-1, 0), new Point(0, 0), new Point(0, -1), new Point(1, -1)},
			{new Point(-1, 0), new Point(0, 0), new Point(0, -1), new Point(1, 0)},
			{new Point(-1, -1), new Point(0, -1), new Point(0, 0), new Point(1, 0)}};
	static Point[][] srs = {
			{new Point(-1, 0), new Point(-1, -1), new Point(0, 2), new Point(-1, 2)}, //0>>1 or 2>>1
			{new Point(1, 0), new Point(1, 1), new Point(0, -2), new Point(1, -2)}, //1>>0 or 1>>2
			{new Point(1, 0), new Point(1, -1), new Point(0, 2), new Point(1, 2)}, //2>>3 or 0>>3
			{new Point(-1, 0), new Point(-1, 1), new Point(0, -2), new Point(-1, -2)}}; //3>>2 or 3>>0
	static final Color[] colors = {Color.black, Color.cyan, Color.blue, Color.orange, Color.yellow, Color.green, Color.magenta, Color.red, Color.gray, Color.white};
	static boolean[] keys = new boolean[65536];
	static Point current = new Point(4, 1);
	static Point mouse = null;
	static int[][] board = new int[20][10];
	static ArrayList<Integer> queue;
	static int[] startQueue;
	private static final long serialVersionUID = 1L;
	static BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_3BYTE_BGR);
	static Image controls = Toolkit.getDefaultToolkit().getImage("controls.jpg");
	static JFrame frame;
	static Canvas canvas;
	static int total = 0;
	static int height = 4;

	//Controls
	static int das = 110;
	static int arr = 0;
	static int pieces = 4;
	
	public static void main(String[] args) throws IOException, InterruptedException{
		frame = new JFrame();
		canvas = new Main();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		canvas.setSize(800, 600);
		frame.add(canvas);
		canvas.setBackground(Color.black);
		frame.add(canvas);
		frame.pack();
		canvas.setFocusable(false);
		canvas.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
			}
			@Override
			public void mousePressed(MouseEvent e) {
				mouse = e.getPoint();
				update = true;
			}
			@Override
			public void mouseReleased(MouseEvent e) {				
			}
			@Override
			public void mouseEntered(MouseEvent e) {				
			}
			@Override
			public void mouseExited(MouseEvent e) {				
			}
		});
		frame.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}
			@Override
			public void keyPressed(KeyEvent e) {
				keys[e.getKeyCode()] = true;
			}
			@Override
			public void keyReleased(KeyEvent e) {
				keys[e.getKeyCode()] = false;
			}
		});
		BufferedReader reader = new BufferedReader(new FileReader("solutions"+pieces+".txt"));
		ArrayList<String> patterns = new ArrayList<String>();
		while (true) {
			String next = reader.readLine();
			if (next==null) {
				break;
			}
			patterns.add(next);
		}
		reader.close();
		total = patterns.size();
		canvas.repaint();
		frame.setVisible(true);
		while (true) {
			board = new int[20][10];
			String pick = patterns.get((int)(Math.random()*total));
			for (int y=20-height; y<20; y++) {
				for (int x=0; x<10; x++) {
					if (x>=pick.charAt(y*2-32)-48&&x<=pick.charAt(y*2-31)-48) {
						board[y][x] = 0;
					} else {
						board[y][x] = 8;
					}
				}
			}
			queue = new ArrayList<Integer>();
			for (int i=height*2; i<height*2+pieces; i++) {
				queue.add(pick.charAt(i)-48);
			}
			Queue<Solution> possible = new LinkedList<Solution>();
			possible.add(new Solution(board));
			for (int i=0; i<pieces; i++) {
				int total = possible.size();
				for (int j=0; j<total; j++) {
					Solution current = possible.poll();
					if (!current.valid) {
						continue;
					}
					ArrayList<Point[]> options = getOptions(current.board, queue.get(i), height-current.lines);
					for (int k=0; k<options.size(); k++) {
						possible.add(new Solution(current.board, options.get(k), current.positions, queue.get(i), current.lines));
					}
				}
			}
			if (possible.isEmpty()) {
				System.out.println("No solution???");
			}
			int holdIndex = -1;
			startQueue = new int[pieces+1];
			for (int i=0; i<pieces; i++) {
				boolean doHold = Math.random()<0.5;
				if (doHold) {
					if (holdIndex>=0) {
						startQueue[holdIndex] = queue.get(i);
						holdIndex = i+1;
					} else {
						holdIndex = i;
						startQueue[i+1] = queue.get(i);
					}
				} else {
					if (holdIndex>=0) {
						startQueue[i+1] = queue.get(i);
					} else {
						startQueue[i] = queue.get(i);
					}
				}
			}
			ArrayList<Integer> remain = new ArrayList<Integer>(Arrays.asList(1,2,3,4,5,6,7));
			for (int i=0; i<pieces; i++) {
				remain.remove((Integer)queue.get(i));
			}
			if (holdIndex==-1) {
				startQueue[pieces] = remain.get((int)(Math.random()*(7-pieces)));
			} else {
				startQueue[holdIndex] = remain.get((int)(Math.random()*(7-pieces)));
			}
			queue.clear();
			for (int i=0; i<pieces+1; i++) {
				queue.add(startQueue[i]);
			}
			hold = 0;
			update = true;
			drawDisplay();
			canvas.paint(canvas.getGraphics());
			while (true) {
				Thread.sleep(Math.max(20, arr));
				if (mouse!=null&&mouse.x>450&&mouse.y>400&&mouse.x<750&&mouse.y<500) {
					keys['G'] = true;
					mouse = null;
				}
				if (showSolution<0) {
					tick();
					if (keys['G']) {
						update = true;
						showSolution = 0;
						solution = System.currentTimeMillis()+500;
						keys['G'] = false;
						board = new int[20][10];
						for (int y=20-height; y<20; y++) {
							for (int x=0; x<10; x++) {
								if (x>=pick.charAt((y-20+height)*2)-48&&x<=pick.charAt((y-20+height)*2+1)-48) {
									board[y][x] = 0;
								} else {
									board[y][x] = 8;
								}
							}
						}
						queue.clear();
						for (int i=0; i<pieces+1; i++) {
							queue.add(startQueue[i]);
						}
					}
				} else {
					if (System.currentTimeMillis()>solution) {
						update = true;
						showSolution++;
						solution+=500;
						if (showSolution%2==1) {
							for (int i=0; i<4; i++) {
								board[possible.peek().positions.get(showSolution/2)[i].y][possible.peek().positions.get(showSolution/2)[i].x] = pick.charAt(showSolution/2+8)-48;
							}							
						} else {
							if (showSolution==pieces*2) {
								showSolution = -1;
								for (int y=20-height; y<20; y++) {
									for (int x=0; x<10; x++) {
										if (x>=pick.charAt((y-20+height)*2)-48&&x<=pick.charAt((y-20+height)*2+1)-48) {
											board[y][x] = 0;
										} else {
											board[y][x] = 8;
										}
									}
								}
								queue.clear();
								for (int i=0; i<pieces+1; i++) {
									queue.add(startQueue[i]);
								}
								current = new Point(4, 1);
							} else {
								update(board);
							}
						}
					}
					if (keys['G']) {
						showSolution = -1;
						for (int y=20-height; y<20; y++) {
							for (int x=0; x<10; x++) {
								if (x>=pick.charAt((y-20+height)*2)-48&&x<=pick.charAt((y-20+height)*2+1)-48) {
									board[y][x] = 0;
								} else {
									board[y][x] = 8;
								}
							}
						}
						queue.clear();
						for (int i=height*2; i<height*2+pieces; i++) {
							queue.add(pick.charAt(i)-48);
						}
						keys['G'] = false;
						update = true;
					}
				}
				if (queue.isEmpty()||(queue.size()==1&&hold==0)) {
					queue.clear();
					boolean clear = true;
					for (int y=0; y<20; y++) {
						for (int x=0; x<10; x++) {
							if (board[y][x]>0) {
								clear = false;
								break;
							}
						}
					}
					if (clear) {
						break;
					} else {
						board = new int[20][10];
						for (int y=20-height; y<20; y++) {
							for (int x=0; x<10; x++) {
								if (x>=pick.charAt((y-20+height)*2)-48&&x<=pick.charAt((y-20+height)*2+1)-48) {
									board[y][x] = 0;
								} else {
									board[y][x] = 8;
								}
							}
						}
						for (int i=0; i<pieces+1; i++) {
							queue.add(startQueue[i]);
						}
						hold = 0;
					}
				}
				drawDisplay();
				canvas.paint(canvas.getGraphics());
			}
		}
	}
	public static void tick() {
		if (queue.size()==0||(queue.size()==1&&hold==0)) {
			queue.clear();
			return;
		}
		Point[] shape = new Point[4];
		shape = rotate(queue.get(0), current, rotation);
		if (mouse!=null) {
			if (mouse.x>450&&mouse.y>100&&mouse.x<550&&mouse.y<200) {
				for (int i=0; i<4; i++) {
					shape[i].x--;
				}
				current.x--;
				if (collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].x++;
					}
					current.x++;
				} else {
					update = true;
				}
			}
			if (mouse.x>550&&mouse.y>100&&mouse.x<650&&mouse.y<200) {
				for (int i=0; i<4; i++) {
					shape[i].y++;
				}
				current.y++;
				if (collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].y--;
					}
					current.y--;
				} else {
					update = true;
				}
			}
			if (mouse.x>650&&mouse.y>100&&mouse.x<750&&mouse.y<200) {
				for (int i=0; i<4; i++) {
					shape[i].x++;
				}
				current.x++;
				if (collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].x--;
					}
					current.x--;
				} else {
					update = true;
				}
			}
			if (mouse.x>450&&mouse.y>200&&mouse.x<550&&mouse.y<300) {
				int moves = 0;
				while (!collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].x--;
					}
					current.x--;
					moves++;
				}
				for (int i=0; i<4; i++) {
					shape[i].x++;
				}
				current.x++;
				if (moves>1) {
					update = true;
				}
			}
			if (mouse.x>550&&mouse.y>200&&mouse.x<650&&mouse.y<300) {
				int moves = 0;
				while (!collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].y++;
					}
					current.y++;
					moves++;
				}
				for (int i=0; i<4; i++) {
					shape[i].y--;
				}
				current.y--;
				if (moves>1) {
					update = true;
				}
			}
			if (mouse.x>650&&mouse.y>200&&mouse.x<750&&mouse.y<300) {
				int moves = 0;
				while (!collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].x++;
					}
					current.x++;
					moves++;
				}
				for (int i=0; i<4; i++) {
					shape[i].x--;
				}
				current.x--;
				if (moves>1) {
					update = true;
				}
			}
			if (mouse.x>450&&mouse.y>300&&mouse.x<550&&mouse.y<400) {
				rotation+=4;
				rotation%=4;
				Point rotate = srs(board, queue.get(0), current, rotation, false);
				if (rotate!=null) {
					rotation--;
					current.translate(rotate.x, rotate.y);
					shape = rotate(queue.get(0), current, rotation);
					update = true;
				}
			}
			if (mouse.x>550&&mouse.y>300&&mouse.x<650&&mouse.y<400) {
				if (!collide(board, rotate(queue.get(0), current, (rotation+2)%4))){
					rotation+=2;
					shape = rotate(queue.get(0), current, rotation%4);
					update = true;
				}
			}
			if (mouse.x>650&&mouse.y>300&&mouse.x<750&&mouse.y<400) {
				rotation+=4;
				rotation%=4;
				Point rotate = srs(board, queue.get(0), current, rotation, true);
				if (rotate!=null) {
					update = true;
					rotation++;
					current.translate(rotate.x, rotate.y);
					shape = rotate(queue.get(0), current, rotation);
				}
			}
			mouse = null;
		}
		if (keys[KeyEvent.VK_LEFT]) {
			if (dasKey==-1) {
				if (dasTime+das<System.currentTimeMillis()) {
					while (!collide(board, shape)) {
						for (int i=0; i<4; i++) {
							shape[i].x--;
						}
						current.x--;
						if (arr>=20) {
							break;
						} else if (!collide(board, shape)) {
							update = true;
						}
					}
					if (collide(board, shape)) {
						for (int i=0; i<4; i++) {
							shape[i].x++;
						}
						current.x++;
					} else {
						update = true;
					}
				}
			} else {
				dasKey = -1;
				dasTime = System.currentTimeMillis();
				for (int i=0; i<4; i++) {
					shape[i].x--;
				}
				current.x--;
				if (collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].x++;
					}
					current.x++;
				} else {
					update = true;
				}
			}
		} else if (dasKey==-1) {
			dasKey = 0;
		}
		if (keys[KeyEvent.VK_RIGHT]) {
			if (dasKey==1) {
				if (dasTime+das<System.currentTimeMillis()) {
					while (!collide(board, shape)) {
						for (int i=0; i<4; i++) {
							shape[i].x++;
						}
						current.x++;
						if (arr>=20) {
							break;
						} else if (!collide(board, shape)) {
							update = true;
						}
					}
					if (collide(board, shape)) {
						for (int i=0; i<4; i++) {
							shape[i].x--;
						}
						current.x--;
					} else {
						update = true;
					}
				}
			} else {
				dasKey = 1;
				dasTime = System.currentTimeMillis();
				for (int i=0; i<4; i++) {
					shape[i].x++;
				}
				current.x++;
				if (collide(board, shape)) {
					for (int i=0; i<4; i++) {
						shape[i].x--;
					}
					current.x--;
				} else {
					update = true;
				}
			}
		} else if (dasKey==1) {
			dasKey = 0;
		}
		if (keys[KeyEvent.VK_UP]) {
			keys[KeyEvent.VK_UP] = false;
			rotation+=4;
			rotation%=4;
			Point rotate = srs(board, queue.get(0), current, rotation, true);
			if (rotate!=null) {
				update = true;
				rotation++;
				current.translate(rotate.x, rotate.y);
				shape = rotate(queue.get(0), current, rotation);
			}
		}
		if (keys[KeyEvent.VK_DOWN]) {
			while (!collide(board, shape)) {
				for (int i=0; i<4; i++) {
					shape[i].y++;
				}
				current.y++;
				if (arr>=20) {
					break;
				} else if (!collide(board, shape)) {
					update = true;
				}
			}
			if (collide(board, shape)) {
				for (int i=0; i<4; i++) {
					shape[i].y--;
				}
				current.y--;
			} else {
				update = true;
			}
		}
		rotation+=4;
		rotation%=4;
		if (keys['S']) {
			keys['S'] = false;
			for (int i=0; i<4; i++) {
				shape[i].y++;
			}
			current.y++;
			if (collide(board, shape)) {
				for (int i=0; i<4; i++) {
					shape[i].y--;
				}
				current.y--;
			} else {
				update = true;
			}
		}
		if (keys['Z']) {
			keys['Z'] = false;
			rotation+=4;
			rotation%=4;
			Point rotate = srs(board, queue.get(0), current, rotation, false);
			if (rotate!=null) {
				rotation--;
				current.translate(rotate.x, rotate.y);
				shape = rotate(queue.get(0), current, rotation);
				update = true;
			}
		}
		rotation+=4;
		rotation%=4;
		if (keys['A']) {
			keys['A'] = false;
			if (!collide(board, rotate(queue.get(0), current, (rotation+2)%4))){
				rotation+=2;
				shape = rotate(queue.get(0), current, rotation%4);
				update = true;
			}
		}
		if (keys['C']) {
			if (canHold) {
				if (hold>0) {
					queue.add(1, hold);
				}
				hold = queue.remove(0);
				update = true;
				canHold = false;
				current = new Point(4, 1);
				rotation = 0;
			}
			return;
		}
		rotation+=4;
		rotation%=4;
		if (keys[' ']) {
			keys[' '] = false;
			update = true;
			while (!collide(board, shape)) {
				for (int i=0; i<4; i++) {
					shape[i].y++;
				}
				current.y++;
			}
			for (int i=0; i<4; i++) {
				shape[i].y--;
			}
			current.y--;
			for (int i=0; i<4; i++) {
				board[shape[i].y][shape[i].x] = queue.get(0);
			}
			queue.remove(0);
			update(board);
			rotation = 0;
			current = new Point(4, 1);
			canHold = true;
		}
	}
	public static int update(int[][] board) {
		int lines = 0;
		for (int y=19; y>=0; y--) {
			for (int x=0; x<10; x++) {
				if (board[y+lines][x]==0) {
					break;
				}
				if (x==9) {
					lines++;
					for (int i=y+lines-1; i>=0; i--) {
						for (int j=0; j<10; j++) {
							if (i>0) {
								board[i][j] = board[i-1][j];
								board[i-1][j] = 0;
							} else {
								board[i][j] = 0;
							}
						}
					}
				}
			}
			
		}
		return lines;
	}
	static class Solution{
		int lines;
		ArrayList<Point[]> positions = new ArrayList<Point[]>();
		boolean valid = true;
		int[][] board = new int[20][10];
		public Solution(int[][] b) {
			for (int y=16; y<20; y++) {
				for (int x=0; x<10; x++) {
					board[y][x] = b[y][x];
				}
			}
		}
		public Solution(int[][] b, Point[] p, ArrayList<Point[]> l, int t, int c) {
			positions = new ArrayList<Point[]>(l);
			positions.add(p);
			lines = c;
			for (int y=20-height; y<20; y++) {
				for (int x=0; x<10; x++) {
					board[y][x] = b[y][x];
				}
			}
			for (int i=0; i<4; i++) {
				board[p[i].y][p[i].x] = t;
				if (p[i].y<20-height+lines) {
					valid = false;
				}
			}
			lines+=update(board);
		}
	}
	public static ArrayList<Point[]> getOptions(int[][] board, int piece, int maxHeight){
		ArrayList<Point[]> options = new ArrayList<Point[]>();
		boolean[][][] visited = new boolean[10][20][4];
		Queue<int[]> visitQueue = new LinkedList<int[]>();
		int[] current = {4, 1, 0};
		visitQueue.add(current);
		while (visitQueue.size()>0) {
			current = visitQueue.poll();
			int[][] moves = legal(board, current, piece);
			if (moves[1]==null) {
				Point[] option = rotate(piece, new Point(current[0], current[1]), current[2]);
				for (int j=0; j<4; j++) {
					if (option[j].y<20-maxHeight) {
						break;
					}
					if (j==3) {
						options.add(option);
					}
				}
			}
			for (int i=0; i<6; i++) {
				if (moves[i]!=null) {
					moves[i][2]+=4;
					moves[i][2]%=4;
					if (!visited[moves[i][0]][moves[i][1]][moves[i][2]]) {
						visitQueue.add(moves[i]);
						visited[moves[i][0]][moves[i][1]][moves[i][2]] = true;
					}
				}
			}
		}
		return options;
	}
	public static int[][] legal(int[][] board, int[] current, int piece) { //left, down, right, cw, 180, ccw
		int[][] options = new int[6][3];
		Point[] location = rotate(piece, new Point(current[0], current[1]), current[2]);
		//left
		Point[] copy = copy(location);
		for (int i=0; i<4; i++) {
			copy[i].translate(-1, 0);
		}
		if (collide(board, copy)) {
			options[0] = null;
		} else {
			options[0][0] = current[0]-1;
			options[0][1] = current[1];
			options[0][2] = current[2];
		}
		//Down
		copy = copy(location);
		for (int i=0; i<4; i++) {
			copy[i].translate(0, 1);
		}
		if (collide(board, copy)) {
			options[1] = null;
		} else {
			options[1][0] = current[0];
			options[1][1] = current[1]+1;
			options[1][2] = current[2];
		}
		//Right
		copy = copy(location);
		for (int i=0; i<4; i++) {
			copy[i].translate(1, 0);
		}
		if (collide(board, copy)) {
			options[2] = null;
		} else {
			options[2][0] = current[0]+1;
			options[2][1] = current[1];
			options[2][2] = current[2];
		}
		//Clockwise
		Point rotated = srs(board, piece, new Point(current[0], current[1]), current[2], true);
		if (rotated==null) {
			options[3] = null;
		} else {
			rotated.translate(current[0], current[1]);
			options[3][0] = rotated.x;
			options[3][1] = rotated.y;
			options[3][2] = current[2]+1;
		}
		if ((piece==1&&current[1]>5)||piece==4) {
			options[3] = null;
		}
		//180
		copy = copy(location);
		for (int i=0; i<4; i++) {
			copy[i].translate(current[0]*-1, current[1]*-1);
			copy[i].x*=-1;
			copy[i].y*=-1;
			copy[i].translate(current[0], current[1]);
		}
		if (collide(board, copy)) {
			options[4] = null;
		} else {
			options[4][0] = current[0];
			options[4][1] = current[1];
			options[4][2] = (current[2]+2)%4;
		}
		if ((piece==1&&current[1]>5)||piece==4) {
			options[4] = null;
		}
		//Counterclockwise
		rotated = srs(board, piece, new Point(current[0], current[1]), current[2], false);
		if (rotated==null) {
			options[5] = null;
		} else {
			rotated.translate(current[0], current[1]);
			options[5][0] = rotated.x;
			options[5][1] = rotated.y;
			options[5][2] = current[2]-1;
		}
		if ((piece==1&&current[1]>5)||piece==4) {
			options[5] = null;
		}
		return options;
	}
	public static Point srs(int[][] board, int piece, Point location, int rotation, boolean cw) {
		rotation+=4;
		rotation%=4;
		Point[] test = rotate(piece, location, rotation+(cw?1:-1));
		if (!collide(board, test)) {
			return new Point(0, 0);
		}
		if ((rotation==0&&cw)||(rotation==2&&!cw)) {
			for (int i=0; i<4; i++) {
				Point[] kick = copy(test);
				for (int j=0; j<4; j++) {
					kick[j].translate(srs[0][i].x, srs[0][i].y);
				}
				if (!collide(board, kick)) {
					return new Point(srs[0][i]);
				}
			}
		}
		if ((rotation==1&&!cw)||(rotation==1&&cw)) {
			for (int i=0; i<4; i++) {
				Point[] kick = copy(test);
				for (int j=0; j<4; j++) {
					kick[j].translate(srs[1][i].x, srs[1][i].y);
				}
				if (!collide(board, kick)) {
					return new Point(srs[1][i]);
				}
			}
		}
		if ((rotation==2&&cw)||(rotation==0&&!cw)) {
			for (int i=0; i<4; i++) {
				Point[] kick = copy(test);
				for (int j=0; j<4; j++) {
					kick[j].translate(srs[2][i].x, srs[2][i].y);
				}
				if (!collide(board, kick)) {
					return new Point(srs[2][i]);
				}
			}
		}
		if ((rotation==3&&!cw)||(rotation==3&&cw)) {
			for (int i=0; i<4; i++) {
				Point[] kick = copy(test);
				for (int j=0; j<4; j++) {
					kick[j].translate(srs[3][i].x, srs[3][i].y);
				}
				if (!collide(board, kick)) {
					return new Point(srs[3][i]);
				}
			}
		}
		return null;
	}
	public static Point[] rotate(int piece, Point location, int rotation) {
		rotation+=4;
		rotation%=4;
		Point[] result = copy(shapes[piece]);
		if (rotation==1) {
			for (int i=0; i<4; i++) {
				int temp = result[i].x;
				result[i].x = result[i].y*-1;
				result[i].y = temp;
			}
		}
		if (rotation==2) {
			for (int i=0; i<4; i++) {
				result[i].x*=-1;
				result[i].y*=-1;
			}
		}
		if (rotation==3) {
			for (int i=0; i<4; i++) {
				int temp = result[i].x;
				result[i].x = result[i].y;
				result[i].y = temp*-1;
			}
		}
		for (int i=0; i<4; i++) {
			result[i].translate(location.x, location.y);
		}
		return result;
	}
	public static boolean collide(int[][] board, Point[] location) {
		for (int i=0; i<4; i++) {
			try {
				if (board[location[i].y][location[i].x]>0) {
					return true;
				}
			} catch (Exception e) {
				return true;
			}
		}
		return false;
	}
	public static Point[] copy(Point[] original) {
		Point[] copy = new Point[original.length];
		for (int i=0; i<original.length; i++) {
			copy[i] = new Point(original[i]);
		}
		return copy;
	}
	public void paint(Graphics g) {
		g.drawImage(image, 0, 0, null);
	}
	public static void drawDisplay() {
		if (!update) {
			return;
		}
		update = false;
		Graphics g = image.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, 800, 600);
		g.drawImage(controls, 450, 100, 300, 400, null);
		g.setColor(Color.white);
		for (int y=0; y<20; y++) {
			for (int x=0; x<10; x++) {
				g.setColor(colors[board[y][x]]);
				g.fillRect(100+x*20, 100+y*20, 20, 20);
			}
		}
		for (int i=1; i<queue.size()&&showSolution<0; i++) {
			int piece = queue.get(i);
			g.setColor(colors[piece]);
			for (int j=0; j<4; j++) {
				g.fillRect(340+shapes[piece][j].x*20, 120+shapes[piece][j].y*20+i*60, 20, 20);
			}
		}
		g.setColor(Color.darkGray);
		for (int x=1; x<10; x++) {
			g.drawLine(x*20+100, 100, x*20+100, 500);
		}
		for (int y=1; y<20; y++) {
			g.drawLine(100, y*20+100, 300, y*20+100);
		}

		Point[] shadow = rotate(queue.get(0), current, rotation);
		while (!collide(board, shadow)) {
			for (int i=0; i<4; i++) {
				shadow[i].translate(0, 1);
			}
		}
		for (int i=0; i<4&&showSolution<0; i++) {
			shadow[i].translate(0, -1);
			g.fillRect(100+shadow[i].x*20, 100+shadow[i].y*20, 20, 20);
		}
		g.setColor(colors[hold]);
		for (int i=0; i<4&&showSolution<0&&hold>0; i++) {
			g.fillRect(30+shapes[hold][i].x*20, 50+10+shapes[hold][i].y*20, 20, 20);
		}
		g.setColor(colors[queue.get(0)]);
		Point[] shape = rotate(queue.get(0), current, rotation);
		for (int i=0; i<4&&showSolution<0; i++) {
			g.fillRect(100+shape[i].x*20, 100+shape[i].y*20, 20, 20);
		}
		//debug
		//g.drawString(current.x+" "+current.y, 50, 550);
	}
}