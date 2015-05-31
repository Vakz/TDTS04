import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FiveInARow
{
  private int size;
  private boolean gameOver = false;
  private char winner = ' ';
  private char[][] grid;
  private int spacesLeft;

  public FiveInARow(int size)
  {
    if (size < 5)
    {
      throw new IllegalArgumentException(
        "Size must be 5 <= x <= 9"
        );
    }
    this.size = size;
    restart();
  }

  public void restart()
  {
    spacesLeft = size*size;
    grid = new char[size][size];
    for(int i = 0; i < size; ++i)
    {
      Arrays.fill(grid[i], ' ');
    }
    gameOver = false;
  }

  public String drawBoard()
  {
    String board = " ABCDEFGHIJ".substring(0, size+1) + "\n";
    for(int i = 0; i < size; ++i)
    {
      board += i;
      for(int j = 0; j < size; ++j)
      {
        board += grid[i][j];
      }
      board += "\n";
    }
    return board;
  }

  private void checkIfOver()
  {
    // Checking horizontal
    checkGrid(grid);

    // Check vertical
    checkGrid(transpose_vertical(grid));

    // Check right diagonal
    checkGrid(transpose_vertical(transpose_diagonal(grid, true)));

    // Check left diagonal
    checkGrid(transpose_vertical(transpose_diagonal(grid, false)));

    if (!gameOver && spacesLeft == 0) gameOver = true;
  }

  private boolean checkGrid(char[][] a)
  {
    for (char[] row : a)
    {
      char c = checkRow(row);
      if(c == 'X' || c == 'O')
      {
        gameOver = true;
        winner = c;
        return true;
      }
    }
    return false;
  }

  private char[][] transpose_vertical(char[][] a)
  {
    char[][] transposed = new char[a.length][a.length];
    for(int i = 0; i < a.length; ++i)
    {
      for(int j = 0; j < a.length; ++j)
      {
        transposed[j][i] = a[i][j];
      }
    }

    return transposed;
  }

  private char[][] transpose_diagonal(char[][] a, boolean left)
  {
    char[][] transposed = new char[a.length][a.length];
    for (int i = 0; i < a.length; ++i)
    {
      int offset = left ? i : a.length-i;
      System.arraycopy(a[i], offset, transposed[i], 0, a[i].length-offset);
      System.arraycopy(a[i], 0, transposed[i], a[i].length-offset, offset);
    }

    return transposed;
  }

  private char checkRow(char row[])
  {
    String row_joined = new String(row);
    Pattern p = Pattern.compile("X{5}");
    Matcher m = p.matcher(row_joined);
    if(m.find())
    {
      return 'X';
    }
    p = Pattern.compile("O{5}");
    m = p.matcher(row_joined);
    if (m.find())
    {
      return 'O';
    }
    return ' ';
  }

  public int getSize()
  {
    return size;
  }

  public char getWinner()
  {
    return winner;
  }

  public boolean isOver()
  {
    return gameOver;
  }

  public boolean placeMarker(int row, int col, char marker)
  {
    char marker_upper = Character.toUpperCase(marker);
    if (marker_upper != 'X' && marker_upper != 'O')
    {
      throw new IllegalArgumentException("Marker must be 'x' or 'o'.");
    }
    if (row < 0 || row > size-1 || col < 0 || col > size-1)
    {
      return false;
    }
    if (grid[col][row] == 'X' || grid[col][row] == 'O')
    {
      return false;
    }
    grid[col][row] = marker_upper;
    --spacesLeft;
    checkIfOver();
    return true;
  }
}
