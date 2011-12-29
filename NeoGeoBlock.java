
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Point;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.awt.image.BufferStrategy;

import java.util.HashSet;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Comparator;

import me.murooka.game.ui.Input;
import me.murooka.game.ui.Mouse;


class NeoGeoBlock {

    public static final int FRAME_WIDTH = 320;
    public static final int FRAME_HEIGHT = 480;

    /**
     * $B$9$Y$F$N;O$^$j!"$=$7$F=*$o$j(B
     */
    public static void main(String[] args) {

        final Game game = new Game();

        @SuppressWarnings("serial") 
        final JFrame frame = new JFrame("Neo Geo Block") {
            // private static final long serialVersionUID = 1L;
            {
                add(new JPanel() {
                    {
                        setPreferredSize(new Dimension(FRAME_WIDTH,FRAME_HEIGHT)); 
                        addMouseListener(new MouseAdapter() {
                            @Override
                            public void mousePressed(MouseEvent e) {
                                Point p = e.getPoint();
                                int id = e.getButton();
                                Input.mouse().press(id, p);
                            }
                            @Override
                            public void mouseReleased(MouseEvent e) {
                                Point p = e.getPoint();
                                int id = e.getButton();
                                Input.mouse().release(id, p);
                            }
                        });
                        addMouseMotionListener(new MouseMotionAdapter() {
                            @Override
                            public void mouseDragged(MouseEvent e) {
                                Point p = e.getPoint();
                                int id = e.getButton();
                                Input.mouse().drag(id, p);
                            }
                        });
                    }
                });
                setVisible(true);
                setResizable(false);
                pack();
                setDefaultCloseOperation(EXIT_ON_CLOSE);
            }

        }; // Jframe


        TimerTask loopTask = new TimerTask() {

            BufferStrategy buffer;
            Insets insets;

            {
                frame.setIgnoreRepaint(true);
                frame.createBufferStrategy(3);
                buffer = frame.getBufferStrategy();
                insets = frame.getInsets();
            }

            @Override
            public void run() {
                // update status
                game.update();

                // update graphics
                Graphics frameGraphics = buffer.getDrawGraphics();
                Graphics g = frameGraphics.create(insets.left, insets.top, FRAME_WIDTH, FRAME_HEIGHT);

                game.renderer().render(g);

                buffer.show();
                g.dispose();
                frameGraphics.dispose();

                try {
                    Thread.sleep(16);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }; // TimerTask

        Timer timer = new Timer("main loop");
        timer.schedule(loopTask, 0, 16);

    }

}



/**
 * NeoGeoBlock$B$NIA<L$rC4Ev$9$k%/%i%9(B
 */
class Renderer {

    public static final int BLOCK_WIDTH = 40;
    public static final int BLOCK_HEIGHT = 40;
    public static final int BOARD_WIDTH = 320;
    public static final int BOARD_HEIGHT = 320;
    public static final int TOP_OFFSET = 80;
    private static final List<Color> colors = Arrays.asList(Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.YELLOW, Color.CYAN);

    private Game game;
    private Board board;

    public Board board() { return this.board; }

    /**
     * @param game
     */
    public Renderer(Game game) {
        this.game = game;
        this.board = game.board();
    }

    /**
     * @param graphics $BIA2hBP>]$N(BGraphics
     */
    public void render(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics;

        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, NeoGeoBlock.FRAME_WIDTH, NeoGeoBlock.FRAME_HEIGHT);

        Graphics2D boardGraphics = (Graphics2D)g.create(0, TOP_OFFSET, BOARD_WIDTH, BOARD_HEIGHT);
        renderBoard(boardGraphics);

    }


    /**
     * render board
     */
    private void renderBoard(Graphics2D g) {

        for (Block b : board()) {
            int x = b.x;
            int y = b.y;
            int w = BLOCK_WIDTH;
            int h = BLOCK_HEIGHT;
            int kind = b.kind;
            Color color = colors.get(kind);
            if (b.fixed()) {
                if (y<board().column()) continue;

                g.setColor(color);
                g.fillRect(x*w+1, (y-board.column())*h+1, w-2, h-2);
            }
        }
        
        // changing blocks
        for (ChangingEvents.Event e : board().changingEvents()) {
            final int w = BLOCK_WIDTH;
            final int h = BLOCK_HEIGHT;
            final int vx = (int)(1.0 * w * e.vx() * e.timing());
            final int vy = (int)(1.0 * h * e.vy() * e.timing());
            {
                int x = e.dst().x;
                int y = e.dst().y;
                int kind = e.dst().kind;
                Color color = colors.get(kind);

                g.setColor(color);
                g.fillRect(x*w+vx+1, (y-board.column())*h+vy+1, w-2, h-2);
            }
            {
                int x = e.src().x;
                int y = e.src().y;
                int kind = e.src().kind;
                Color color = colors.get(kind);

                g.setColor(color);
                g.fillRect(x*w-vx+1, (y-board.column())*h-vy+1, w-2, h-2);
            }
        }

        // erasing blocks
        for (ErasingEvents.Event e : board().erasingEvents()) {
            final int w = BLOCK_WIDTH;
            final int h = BLOCK_HEIGHT;
            final int ox = (int)(e.timing() * w / 2);
            final int oy = (int)(e.timing() * h / 2);
            final double timing = 1.0 - e.timing();
            for (Block b : e.blocks()) {
                final int x = b.x;
                final int y = b.y;
                final int kind = b.kind;
                final Color color = colors.get(kind);

                g.setColor(color);
                g.fillRect(x*w+ox+1, (y-board.column())*h+oy+1, (int)(w*timing)-2, (int)(h*timing)-2);
            }
        }

        // falling blocks
        for (FallingEvents.Event e : board().fallingEvents()) {
            final Block block = e.block();
            final int w = BLOCK_WIDTH;
            final int h = BLOCK_HEIGHT;
            final int x = block.x;
            final int y = block.y;
            final int offset = e.offset();
            final int kind = block.kind;
            final Color color = colors.get(kind);

            g.setColor(color);
            g.fillRect(x*w+1, (y-board.column())*h+offset+1, w-2, h-2);
        }

    }


    /**
     * $B%&%#%s%I%&>e$N@dBP:BI8$r<u$1<h$j!"%V%m%C%/$N:BI8$rJV$9(B
     *
     * $B%&%#%s%I%&$KI=<($5$l$F$$$J$/$F$b!"(BBoard$B%/%i%9$G$=$N>l=j$K%V%m%C%/$,Dj5A$5$l$F$$$l$P!"$=$N%V%m%C%/:BI8$rJV$9(B
     * $B%V%m%C%/$,B8:_$7$F$$$J$1$l$P(Bnull$B$rJV$9(B
     *
     * @param x $B%&%#%s%I%&>e$N(Bx$B:BI8(B
     * @param y $B%&%#%s%I%&>e$N(By$B:BI8(B
     * @return $B%V%m%C%/$N:BI8(B
     */
    public Block blockAt(int x, int y) {
        int bx = x / BLOCK_WIDTH;
        int by = (y-TOP_OFFSET) / BLOCK_HEIGHT + 8;

        Block res = null;
        if (0<=bx && bx<board.row() && 0<=by && by<board.column()*2) {
            res = board.block(bx, by);
        }
        return res;
    }

}








/**
 * NeoGeoBlock$B$G$N%V%m%C%/(B1$B$DJ,$KAjEv$9$k%/%i%9(B
 */
class Block {
    public static final int None   = 0;
    public static final int Red    = 1;
    public static final int Green  = 2;
    public static final int Blue   = 3;
    public static final int Magenta= 4;
    public static final int Yellow = 5;
    public static final int Cyan   = 6;
    private static final List<String> colorName = Arrays.asList("None","Red","Green","Blue","Magenta","Yellow","Cyan");

    private static final int FIXED = 1;
    private static final int CHANGING = 2;
    private static final int ERASING = 3;
    private static final int FALLING = 4;
    private static final List<String> stateName = Arrays.asList("Dummy", "Fixed", "Changing", "Erasing", "Falling");

    int kind = None;
    int x = -1;
    int y = -1;

    private int state = FIXED;
    boolean fixed() { return state==FIXED && kind!=None; }
    boolean changing() { return state==CHANGING && kind!=None; }
    boolean erasing() { return state==ERASING; }
    boolean falling() { return state==FALLING; }



    void fix() { state = FIXED; }
    void change() { assert state==FIXED; state = CHANGING; }
    void erase() { state = ERASING; }
    void fall() { state = FALLING; }

    /**
     * @param kind $B%V%m%C%/$N<oN`(B
     * @param p $B%V%m%C%/$N:BI8(B
     */
    Block(int kind, Point p) {
        this(kind, p.x, p.y);
    }

    /**
     * @param kind $B%V%m%C%/$N<oN`(B
     * @param x x$B:BI8(B
     * @param y y$B:BI8(B
     */
    Block(int kind, int x, int y) {
        this.kind = kind;
        this.x = x;
        this.y = y;
    }


    /**
     * $BJ8;zNs$KJQ49$9$k(B
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Block[").append(colorName.get(kind));
        sb.append(",").append(stateName.get(state));
        sb.append(",x=").append(x);
        sb.append(",y=").append(y);
        sb.append("]");
        return sb.toString();
    }

}



/**
 * $B%V%m%C%/$N=89g(B
 */
class Board implements Iterable<Block> {
    
    HashSet<Block> blocks;
    boolean[][] cache;

    private int row;
    private int column;
    private ChangingEvents changingEvents;
    private ErasingEvents erasingEvents;
    private FallingEvents fallingEvents;
    private Rectangle visibleBlockRect;

    public int row() { return this.row; }
    public int column() { return this.column; }
    public ChangingEvents changingEvents() { return this.changingEvents; }
    public ErasingEvents erasingEvents() { return this.erasingEvents; }
    public FallingEvents fallingEvents() { return this.fallingEvents; }
    public Rectangle visibleBlockRect() { return this.visibleBlockRect; }


    /**
     * $B%G%U%)%k%H%3%s%9%H%i%/%?(B
     *
     * @param row $B2#$KJB$V%V%m%C%/$N8D?t(B
     * @param column $B=D$KJB$V%V%m%C%/$N8D?t(B
     * @param kinds $B%V%m%C%/$N<oN`(B
     */
    Board(int row, int column, int kinds) {
        this.row = row;
        this.column = column;
        this.changingEvents = new ChangingEvents(this);
        this.erasingEvents = new ErasingEvents();
        this.fallingEvents = new FallingEvents(this);
        this.visibleBlockRect = new Rectangle(0, column, row, column);

        this.blocks = new HashSet<Block>(row*column*2);
        this.cache = new boolean[row][];
        for (int x=0; x<row; x++) this.cache[x] = new boolean[column*2];


        // initialize random blocks
        Block[][] tempBlocks = new Block[row][];
        for (int x=0; x<row; x++) tempBlocks[x] = new Block[column*2];

        Random r = new Random();
        for (int x=0; x<row; x++) {
            for (int y=0; y<column*2; y++) {

                while (true) {
                    int kind = r.nextInt(kinds) + 1;
                    boolean horFlag = x<2 || tempBlocks[x-1][y].kind!=kind || tempBlocks[x-2][y].kind!=kind;
                    boolean verFlag = y<2 || tempBlocks[x][y-1].kind!=kind || tempBlocks[x][y-2].kind!=kind;

                    if (horFlag && verFlag) {
                        tempBlocks[x][y] = new Block(kind, x, y);
                        break;
                    }
                } // while (true)

            }
        }

        for (int x=0; x<row; x++) {
            for (int y=0; y<column*2; y++) {
                blocks.add(tempBlocks[x][y]);
            }
        }


    }


    /** 
     * $B;XDj$7$?:BI8$K;XDj$7$?%V%m%C%/$r%;%C%H$9$k(B
     *
     * @param x x$B:BI8(B
     * @param y y$B:BI8(B
     * @param b $B%V%m%C%/(B
     */
    public void setBlock(int x, int y, Block b) {
        Block old = block(x,y);
        if (old!=null) {
            blocks.remove(b);
        }
        blocks.add(b);
    }

    /**
     * $B;XDj$7$?:BI8$K$"$k%V%m%C%/$r<hF@$9$k(B
     *
     * $B%V%m%C%/$,B8:_$7$J$$:BI8$J$i!"(Bnull$B$rJV$9(B
     *
     * @param x x$B:BI8(B
     * @param y y$B:BI8(B
     * @return $B%V%m%C%/(B
     */
    public Block block(int x, int y) {
        Block block = null;
        for (Block b : blocks) {
            if (b.x==x && b.y==y) {
                block = b;
                break;
            }
        }
        return block;
    }

    public Block block(Point p) {
        return this.block(p.x, p.y);
    }



    /**
     * $B;XDj$7$?(B2$B$D$N%V%m%C%/$rF~$lBX$(>uBV$K$9$k(B
     *
     * @param src
     * @param dst
     */
    public void changeBlocks(Block src, Block dst) {
        this.changingEvents.addEvent(src, dst);
    }


    @Override
    public Iterator<Block> iterator() {
        return blocks.iterator();
    }

    /**
     * $B%\!<%I$r%\!<%I$rJ8;zNsI=5-$K$9$k(B
     *
     * $B%V%m%C%/$r1Q;z0lJ8;z$GI=$7!"(BAA$B$G%\!<%I$N$h$&$KI=<($9$k(B
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y=column; y<column*2; y++) {
            for (int x=0; x<row; x++) {
                sb.append(block(x,y).toString()).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }


}




class ChangingEvents implements Iterable<ChangingEvents.Event> {

    private static final int CHANGE_TIME = 30;

    private Board board;
    private List<Event> events;

    public ChangingEvents(Board board) {
        this.board = board;
        this.events = new ArrayList<Event>();
    }

    public void addEvent(Block src, Block dst) {
        this.events.add(new Event(src, dst, board));
    }

    public void update() {
        List<Event> removes = new ArrayList<Event>();
        for (Event e : this.events) {
            if (e.isEnded()) {
                removes.add(e);
            } else {
                e.update();
            }
        }
        this.events.removeAll(removes);
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }



    static class Event {

        private Block src;
        private Block dst;
        private Board board;
        private int count;
        private int vx, vy;

        public Event(Block src, Block dst, Board board) {
            this.src = src;
            this.dst = dst;
            this.board = board;
            this.count = 0;

            this.vx = src.x - dst.x;
            this.vy = src.y - dst.y;

            this.src.change();
            this.dst.change();
        }

        public Block src() { return this.src; }
        public Block dst() { return this.dst; }
        public int count() { return this.count; }
        public int vx() { return this.vx; }
        public int vy() { return this.vy; }

        public void update() {
            this.count++;
        }

        public double timing() {
            final int count = this.count<CHANGE_TIME ? this.count : CHANGE_TIME*2-this.count;
            return 1.0 * count / CHANGE_TIME;
        }

        public boolean isEnded() {
            boolean res = false;

            if (this.count==ChangingEvents.CHANGE_TIME) {
                this.swap();
                this.fix();

                if (this.isLined()) {
                    res = true;
                } else {
                    this.unfix();
                    this.swap();
                }
            } else if (this.count==ChangingEvents.CHANGE_TIME*2) {
                this.fix();
                res = true;
            }
            
            return res;
        }

        private void swap() {
            int kind = this.src.kind;
            this.src.kind = this.dst.kind;
            this.dst.kind = kind;
        }

        private void fix() {
            this.src.fix();
            this.dst.fix();
        }

        private void unfix() {
            this.src.change();
            this.dst.change();
        }


        private boolean isLined() {
            return this.isLined(src) || this.isLined(dst);
        }

        private boolean isLined(Block b) {
            final int kind = b.kind;
            final int x = b.x;
            final int y = b.y;
            final Point p = new Point(x,y);

            // $B2#J}8~$K$D$$$FD4$Y$k(B
            final int horCount = 1 + this.countSameKind(kind, p, 1, 0) + this.countSameKind(kind, p, -1, 0);
            if (horCount>=3) return true;

            // $B=DJ}8~$K$D$$$FD4$Y$k(B
            final int verCount = 1 + this.countSameKind(kind, p, 0, 1) + this.countSameKind(kind, p, 0, -1);
            if (verCount>=3) return true;

            return false;
        }

        private int countSameKind(int kind, Point p, int vx, int vy) {
            Point nextp = new Point(p.x+vx, p.y+vy);
            if (!this.board.visibleBlockRect().contains(nextp) || this.board.block(nextp).kind!=kind) {
                return 0;
            }
            return 1 + countSameKind(kind, nextp, vx, vy);
        }

    }

}




class ErasingEvents implements Iterable<ErasingEvents.Event> {

    public static final int ERASE_TIME = 60;

    private List<Event> events;

    public ErasingEvents() {
        this.events = new ArrayList<Event>();
    }

    public void addEvent(List<Block> targets) {
        this.events.add(new Event(targets));
    }

    public void update() {
        ArrayList<Event> removes = new ArrayList<Event>();
        for (Event e : this.events) {
            if (e.isEnded()) {
                removes.add(e);
            } else {
                e.update();
            }
        }
        this.events.removeAll(removes);
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }


    static class Event {

        private List<Block> targets;
        private int count;

        public List<Block> blocks() { return this.targets; }

        public Event(List<Block> targets) {
            this.targets = targets;
            this.count = 0;

            for (Block b : this.targets) {
                b.erase();
            }
        }

        public void update() {
            this.count++;
        }

        public double timing() {
            return 1.0 * this.count / ERASE_TIME;
        }

        public boolean isEnded() {
            boolean res = false;
            if (this.count==ERASE_TIME) {
                for (Block b : targets) {
                    b.kind = Block.None;
                    b.fix();
                }
                res = true;
            }
            return res;
        }

    }

}


class FallingEvents implements Iterable<FallingEvents.Event> {

    private Board board;
    private Collection<Event> events;

    public FallingEvents(Board board) {
        this.board = board;
        this.events = new TreeSet<Event>(new Comparator<Event>() {
            @Override
            public int compare(Event lhs, Event rhs) {
                int res = lhs.block().y - rhs.block().y;
                if (res==0) res = lhs.block().x - rhs.block().x;
                return res;
            }
        });
    }

    public void addEvent(List<Block> floatingBlocks) {
        for (Block b : floatingBlocks) {
            this.events.add(new Event(this.board, b));
        }
    }

    public void update() {
        ArrayList<Event> removes = new ArrayList<Event>();
        for (Event e : events) {
            if (e.isEnded()) {
                removes.add(e);
            } else {
                e.update();
            }
        }
        events.removeAll(removes);
    }

    @Override
    public Iterator<Event> iterator() {
        return events.iterator();
    }

    static class Event {

        private Block block;
        private Board board;
        private int count;

        public Block block() { return this.block; }
        private Board board() { return this.board; }

        public Event(Board board, Block block) {
            this.block = block;
            this.board = board;
            this.count = 0;
            this.block.fall();
        }

        public void update() {
            this.count++;
        }

        public int offset() {
            return (int)(Math.pow(count,2));
        }

        public boolean isEnded() {
            boolean res = false;
            final int x = block().x;
            final int y = block().y;
            final int below = y + offset() / Renderer.BLOCK_HEIGHT + 1;
            final Block bb = board().block(x, below);

            boolean landed = false;
            if (y==below) {
                landed = false;
            } else if (below>=board().column()*2) {
                landed = true;
            } else {
                if (bb.fixed() || bb.changing() || bb.erasing()) landed = true;
            }
            if (landed) {
                final Block target = board().block(x, below-1);
                assert target.kind==Block.None : "required None, but " + target;
                assert block().kind!=Block.None : "required not None, but " + target;
                target.kind = block().kind;
                target.fix();
                block().kind = Block.None;
                res = true;
            }

            return res;
        }

    }

}






class Game {

    private Board board;
    private Renderer renderer;
    private Block focused;

    public Board board() { return this.board; }

    public Game() {
        this.board = new Board(8, 8, 3);
        this.renderer = new Renderer(this);
        this.focused = null;
    }


    public Renderer renderer() {
        return renderer;
    }


    public void update() {
        // Inputs update
        Input.update();

        if (Input.mouse().isPressed(Mouse.BUTTON1)) {
            Point p = Input.mouse().pressedPoint(Mouse.BUTTON1);
            Block block = renderer.blockAt(p.x, p.y);


            if (block!=null && board().column()<=block.y && block.fixed()) {
                this.focused = block;
            }
        } else if (Input.mouse().isReleased(Mouse.BUTTON1) && this.focused!=null) {
            Point p = Input.mouse().releasedPoint(Mouse.BUTTON1);
            Block block = renderer.blockAt(p.x, p.y);
            if (block!=null && block.fixed()) {
                int diffx = Math.abs(focused.x - block.x);
                int diffy = Math.abs(focused.y - block.y);
                if (diffx+diffy==1) {
                    board().changingEvents().addEvent(this.focused, block);
                    this.focused = null;
                }
            }
        } else if (Input.mouse().isPressing(Mouse.BUTTON1) && this.focused!=null) {
            Point p = Input.mouse().currentPoint(Mouse.BUTTON1);
            Block block = renderer.blockAt(p.x, p.y);
            if (block!=null && block.fixed()) {
                int diffx = Math.abs(focused.x - block.x);
                int diffy = Math.abs(focused.y - block.y);
                if (diffx+diffy==1) {
                    board().changingEvents().addEvent(this.focused, block);
                    this.focused = null;
                }
            }
        }


        // erase lined blocks
        List<Block> linedBlocks = getLinedBlocks();
        if (!linedBlocks.isEmpty()) board().erasingEvents().addEvent(linedBlocks);

        // fall floating blocks
        List<Block> floatingBlocks = getFloatingBlocks();
        if (!floatingBlocks.isEmpty()) board().fallingEvents().addEvent(floatingBlocks);

        // event update
        board().changingEvents().update();
        board().erasingEvents().update();
        board().fallingEvents().update();

    }





    private List<Block> getLinedBlocks() {
        HashSet<Block> targets = new HashSet<Block>();
        for (int x=0; x<board().row(); x++) {
            int kind = Block.None;
            int counter = 0;
            int y;
            for (y=board().column(); y<board().column()*2; y++) {
                Block b = board().block(x,y);
                if (!b.fixed()) {
                    if (counter>=3) {
                        for (int i=1; i<=counter; i++) targets.add(board().block(x, y-i));
                    }
                    counter = 0;
                } else if (b.kind!=kind) {
                    if (counter>=3) {
                        for (int i=1; i<=counter; i++) targets.add(board().block(x, y-i));
                    }
                    counter = 1;
                    kind = b.kind;
                } else {
                    counter++;
                }
            }
            if (counter>=3) {
                for (int i=1; i<=counter; i++) targets.add(board().block(x, y-i));
            }
        }
        for (int y=board().column(); y<board().column()*2; y++) {
            int kind = Block.None;
            int counter = 0;
            int x;
            for (x=0; x<board().row(); x++) {
                Block b = board().block(x,y);
                if (!b.fixed()) {
                    if (counter>=3) {
                        for (int i=1; i<=counter; i++) targets.add(board().block(x-i, y));
                    }
                    counter = 0;
                } else if (b.kind!=kind) {
                    if (counter>=3) {
                        for (int i=1; i<=counter; i++) targets.add(board().block(x-i, y));
                    }
                    counter = 1;
                    kind = b.kind;
                } else {
                    counter++;
                }
            }
            if (counter>=3) {
                for (int i=1; i<=counter; i++) targets.add(board().block(x-i, y));
            }
        }
        return new ArrayList<Block>(targets);
    }



    /**
     * $BIb$$$F$$$k!J2<$K$"$k%V%m%C%/$,(BNone$B$G$"$k!K%V%m%C%/$r%j%9%H$GJV$9(B
     *
     * @return $BIb$$$F$$$k%V%m%C%/$N%j%9%H(B
     */
    private List<Block> getFloatingBlocks() {
        ArrayList<Block> floatingBlocks = new ArrayList<Block>();
        for (int x=0; x<board().row(); x++) {
            boolean floating = false;
            for (int y=board().column()*2-2; y>=0; y--) {
                Block block = board().block(x,y);

                if (floating && block.fixed()) {
                    floatingBlocks.add(block);
                } else {
                    floating = block.kind==Block.None || block.falling();
                }

            }
        }
        return floatingBlocks;
    }


}
