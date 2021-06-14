package com.example.game_2048

import android.animation.IntArrayEvaluator
import android.content.Context
import android.content.DialogInterface
import android.gesture.Gesture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Layout
import android.util.Half.toFloat
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GestureDetectorCompat
import java.util.Arrays.copyOf
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    /**
     * 操作する方向の列挙型
     */
    enum class Direction { UP, DOWN, RIGHT, LEFT }

    /**
     * ゲームステータスを表す列挙型
     */
    enum class GameStatus { WIN, LOSS, PLAYING }

    /** 待ち時間 */
    val WAIT_TIME = 100    // 0.1sec

    /** フリックの幅 */
    val FLICK_WIDTH = 5

    /** 正方形の数 */
    val SQUARES = 4
    private lateinit var mDetector: GestureDetectorCompat
    private var myContext = this
    val mHandler = Handler()
    private var mWaiting = false
    private var mGameStatus = GameStatus.PLAYING
    private var mFlickMin = 0.0f

    /** 4x4のフィールドを表す配列 */
    private val board = Array(SQUARES) { arrayOfNulls<Int?>(SQUARES) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDetector = GestureDetectorCompat(this, MyGestureListener())
        val buttonRestart = findViewById<Button>(R.id.buttonRestart)
        buttonRestart.setOnClickListener {
            initBoard()
            dispBoard()
        }
        initBoard()
        dispBoard()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        adustLayout()
    }

    private fun adustLayout() {
        val linearLayout: Array<LinearLayout> = arrayOf(
            findViewById(R.id.LinearLayout1),
            findViewById(R.id.LinearLayout2),
            findViewById(R.id.LinearLayout3),
            findViewById(R.id.LinearLayout4)
        )
        val location = IntArray(2) { 0 }
        linearLayout[0].getLocationOnScreen(location)
        var width = linearLayout[0].width
        for (i in 0 until SQUARES) {
            val marginLayoutParams: ViewGroup.MarginLayoutParams =
                linearLayout[i].layoutParams as ViewGroup.MarginLayoutParams
            marginLayoutParams.height = width / SQUARES
            linearLayout[i].layoutParams = marginLayoutParams
        }
        mFlickMin = (width / SQUARES).toFloat()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        this.mDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            event1: MotionEvent,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (mWaiting) return true
            var diffX = event2.x - event1.x
            var diffY = event2.y - event1.y
            var absX = abs(diffX)
            var absY = abs(diffY)
            lateinit var direction: Direction
            // when に置き換える必要がある
            if (FLICK_WIDTH * absX < absY) {
                if (absY < mFlickMin) return true
                direction = if (diffY > 0) {
                    Direction.DOWN
                } else {
                    Direction.UP
                }
            } else if (absX > FLICK_WIDTH * absY) {
                if (absX < mFlickMin) return true
                direction = if (diffX > 0) {
                    Direction.RIGHT
                } else {
                    Direction.LEFT
                }
            } else {
                return true
            }
            val board2 = Array(SQUARES) { arrayOfNulls<Int>(SQUARES) }
            copyBoard(board, board2)
            packBefore(direction)
            synthetic(direction)
            if (!diffBoard(board, board2)) {
                return true
            }
            packBefore(direction)
            dispBoard()
            waitTime()
            return true
        }
    }

    private fun waitTime() {
        mWaiting = true
        Handler().postDelayed({
            mWaiting = false
            if (mGameStatus == GameStatus.WIN) {
                initBoard()
                mGameStatus = (GameStatus.PLAYING)
                AlertDialog.Builder(myContext)
                    .setMessage("YOUR WIN!")
                    .setPositiveButton("OK") { _, _ ->
                        dispBoard()
                    }
                    .show()
                return@postDelayed
            }
            setPiece()
            checkLoss()
            dispBoard()
            if (mGameStatus == GameStatus.LOSS) {
                initBoard()
                mGameStatus = GameStatus.PLAYING
                AlertDialog.Builder(myContext)
                    .setMessage("YOUR LOSS!")
                    .setPositiveButton("OK") { _, _ ->
                        dispBoard()
                    }
                    .show()
            }
        }, WAIT_TIME.toLong())
    }

    private fun initBoard() {
        for (x in 0 until SQUARES) {
            for (y in 0 until SQUARES) {
                board[x][y] = 0
            }
        }
        setPiece()
        setPiece()
    }

    private fun dispBoard() {
        val imageView: Array<ImageView?> = arrayOf(
            findViewById(R.id.imageView11),
            findViewById(R.id.imageView12),
            findViewById(R.id.imageView13),
            findViewById(R.id.imageView14),
            findViewById(R.id.imageView21),
            findViewById(R.id.imageView22),
            findViewById(R.id.imageView23),
            findViewById(R.id.imageView24),
            findViewById(R.id.imageView31),
            findViewById(R.id.imageView32),
            findViewById(R.id.imageView33),
            findViewById(R.id.imageView34),
            findViewById(R.id.imageView41),
            findViewById(R.id.imageView42),
            findViewById(R.id.imageView43),
            findViewById(R.id.imageView44)
        )

        val imageFile = arrayOf(
            R.drawable.p0,
            R.drawable.p2,
            R.drawable.p4,
            R.drawable.p8,
            R.drawable.p16,
            R.drawable.p32,
            R.drawable.p64,
            R.drawable.p128,
            R.drawable.p256,
            R.drawable.p512,
            R.drawable.p1024,
            R.drawable.p2048
        )

        var i = 0

        // 順番にviewに画像をセットする
        for (x in 0 until SQUARES) {
            for (y in 0 until SQUARES) {
                imageView[i++]?.setImageResource(imageFile[board[x][y]!!])
                if (board[x][y] == 11) {
                    mGameStatus = GameStatus.WIN
                }
            }
        }
    }

    private fun packBefore(direction: Direction) {
        var i = 0
        if (direction == Direction.DOWN || direction == Direction.UP) {
            for (y in 0 until SQUARES) {
                var tmp = arrayOf(0, 0, 0, 0)
                if (direction == Direction.DOWN) {
                    var i = SQUARES - 1
                    for (x in (SQUARES - 1) downTo 0) {
                        if (board[x][y] == 0) {
                            continue
                        }
                        tmp[i--] = board[x][y]!!
                    }
                } else {
                    i = 0
                    for (x in 0 until SQUARES) {
                        if (board[x][y] == 0) {
                            continue
                        }
                        tmp[i++] = board[x][y]!!
                    }
                }
                for (x in 0 until SQUARES) {
                    board[x][y] = tmp[x]
                }
            }
        } else {
            for (x in 0 until SQUARES) {
                val tmp = arrayOf(0, 0, 0, 0)
                if (direction == Direction.RIGHT) {
                    i = 3
                    for (y in (SQUARES - 1) downTo 0) {
                        if (board[x][y] == 0) {
                            continue
                        }
                        tmp[i--] = board[x][y]!!
                    }
                } else {
                    i = 0
                    for (y in 0 until SQUARES) {
                        if (board[x][y] == 0) {
                            continue
                        }
                        tmp[i++] = board[x][y]!!
                    }
                }
                System.arraycopy(tmp, 0, board[x], 0, SQUARES)
            }
        }
    }

    private fun synthetic(direction: Direction) {
        var i = 0
        if (direction == Direction.DOWN || direction == Direction.UP) {
            for (y in 0 until SQUARES) {
                if (direction == Direction.DOWN) {
                    for (x in (SQUARES - 1) downTo 1) {
                        if (board[x][y] == 0) continue
                        if (board[x][y] == board[x - 1][y]) {
                            board[x][y] = board[x][y]?.plus(1)
                            board[x - 1][y] = 0
                        }
                    }
                } else {
                    for (x in 0 until (SQUARES - 1)) {
                        if (board[x][y] == 0) continue
                        if (board[x][y] == board[x + 1][y]) {
                            board[x][y] = board[x][y]?.plus(1)
                            board[x + 1][y] = 0
                        }
                    }
                }
            }
        } else {
            for (x in 0 until SQUARES) {
                if (direction == Direction.RIGHT) {
                    for (y in (SQUARES - 1) downTo 1) {
                        if (board[x][y] == 0) continue
                        if (board[x][y] == board[x][y - 1]) {
                            board[x][y] = board[x][y]?.plus(1)
                            board[x][y - 1] = 0
                        }
                    }
                } else {
                    for (y in 0 until SQUARES) {
                        if (board[x][y] == 0) continue
                        if (board[x][y] == board[x][y + 1]) {
                            board[x][y] = board[x][y]?.plus(1)
                            board[x][y + 1] = 0
                        }
                    }
                }
            }
        }
    }

    private fun setPiece() {
        val zeroX = mutableListOf<Int>()
        var zeroY = mutableListOf<Int>()
        var i = 0
        for (x in 0 until SQUARES) {
            for (y in 0 until SQUARES) {
                if (board[x][y] == 0) {
                    zeroX.add(x)
                    zeroY.add(y)
                }
            }
        }
        if (zeroX.size == 0) return
        i = (Math.random() * zeroX.size).toInt()
        board[zeroX[i]][zeroY[i]] = 1
    }


    private fun copyBoard(s: Array<Array<Int?>>, d: Array<Array<Int?>>): Boolean {
        for (x in 0 until SQUARES) {
            for (y in 0 until SQUARES) {
                if (d[x][y] != s[x][y]) {
                    return true
                }
            }
        }
        return false
    }

    private fun diffBoard(s: Array<Array<Int?>>, d: Array<Array<Int?>>): Boolean {
        for (x in 0 until SQUARES) {
            for (y in 0 until SQUARES) {
                if (d[x][y] != s[x][y]) {
                    return true
                }
            }
        }
        return false
    }

    private fun checkLoss() {
        for (x in 0 until SQUARES) {
            for (y in 0 until SQUARES) {
                if (board[x][y] == 0) {
                    return
                }
            }
        }

        for (i in 0 until SQUARES) {
            for (j in 0 until SQUARES) {
                if (board[i][j] == board[i][j + 1]) return
                if (board[j][i] == board[j + 1][i]) return
            }
        }

        for (i in 0 until SQUARES) {
            for (j in 0 until SQUARES) {
                if (board[i][j] == board[i][j + 1]) return
                if (board[i][j] == board[j + 1][i]) return
            }
        }
        mGameStatus = GameStatus.LOSS
    }

}