package im.r_c.android.jigsaw;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Arrays;

import im.r_c.android.commonrecyclerviewadapter.CommonRecyclerViewAdapter;
import im.r_c.android.commonrecyclerviewadapter.ViewHolder;
import im.r_c.android.jigsaw.util.DensityUtils;
import me.drakeet.mailotto.Mail;
import me.drakeet.mailotto.Mailbox;

/**
 * Jigsaw
 * Created by richard on 16/5/15.
 */
public class GameFragment extends Fragment implements View.OnTouchListener {
    private static final String TAG = "GameFragment";
    public static final int DIRECTION_DOWN = 100;
    public static final int DIRECTION_UP = -100;
    public static final int DIRECTION_RIGHT = 101;
    public static final int DIRECTION_LEFT = -101;

    public static GameFragment newInstance(Bitmap[] bitmapBricks, int[][] goalStatus) {
        Bundle args = new Bundle();
        args.putSerializable("bricks", bitmapBricks);
        args.putSerializable("goal", goalStatus);
        GameFragment fragment = new GameFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public GameFragment() {
    }

    private int mSpanCount = 0;
    private Bitmap[] mBitmapBricks = null;
    private int[][] mGoalStatus = null;
    private int[][] mCurrStatus = null;
    private Point mCurrBlankPos = null;
    private RecyclerView mRecyclerView = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        mGoalStatus = (int[][]) args.getSerializable("goal");
        mCurrStatus = copyStatus(mGoalStatus);
        mCurrBlankPos = findBlankBrick(mCurrStatus);
        mBitmapBricks = (Bitmap[]) args.getSerializable("bricks");
        mSpanCount = mGoalStatus.length;

        generateRandomStatus();
        Mailbox.getInstance().post(new Mail(GameActivity.MAIL_GAME_STARTED, GameActivity.class, GameFragment.class));

        View view = inflater.inflate(R.layout.fragment_game, container, false);
        initView(view);
        return view;
    }

    @SuppressWarnings("unchecked")
    private void initView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.rv_bricks);
        assert mRecyclerView != null;
        mRecyclerView.setAdapter(new CommonRecyclerViewAdapter(getContext(), mBitmapBricks, R.layout.brick_item) {
            @Override
            public void onItemViewAppear(ViewHolder holder, Object o, int position) {
                int row = position / mSpanCount;
                int column = position % mSpanCount;
                holder.setViewImageBitmap(R.id.iv_brick, mBitmapBricks[mCurrStatus[row][column]]);
            }
        });
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), mSpanCount) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        mRecyclerView.addItemDecoration(new SquareGridSpacingItemDecoration(getContext(), R.dimen.brick_divider_width, mGoalStatus.length));
        mRecyclerView.setOnTouchListener(this);
    }

    private float mLastX = 0;
    private float mLastY = 0;
    private int mLastDirection = 0;
    private boolean mMovedOnTouch = false;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = rawX;
                mLastY = rawY;
                break;
            case MotionEvent.ACTION_MOVE:
                int offsetX = (int) (rawX - mLastX);
                int offsetY = (int) (rawY - mLastY);
                int slop = DensityUtils.dp2px(getContext(), 80);
                if (Math.abs(offsetX) > slop || Math.abs(offsetY) > slop
                        && !(Math.abs(offsetX) > slop && Math.abs(offsetY) > slop)) {
                    int direction = 0;
                    if (Math.abs(offsetX) > slop) {
                        direction = offsetX > 0 ? DIRECTION_LEFT : DIRECTION_RIGHT;
                    } else if (Math.abs(offsetY) > slop) {
                        direction = offsetY > 0 ? DIRECTION_UP : DIRECTION_DOWN;
                    }

                    if (!mMovedOnTouch || direction == -mLastDirection) {
                        moveBlankBrick(direction, true);
                        mLastDirection = direction;
                        mMovedOnTouch = true;
                        mLastX = rawX;
                        mLastY = rawY;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mMovedOnTouch) {
                    handleMoved();
                    mMovedOnTouch = false;
                }
                break;
        }
        return false;
    }

    private void handleMoved() {
        Mailbox.getInstance().post(new Mail(GameActivity.MAIL_STEP_MOVED, GameActivity.class, GameFragment.class));

        if (isWon()) {
            Mailbox.getInstance().post(new Mail(GameActivity.MAIL_GAME_WON, GameActivity.class, GameFragment.class));
        }
    }

    private boolean isWon() {
        for (int i = 0; i < mSpanCount; i++) {
            if (!Arrays.equals(mCurrStatus[i], mGoalStatus[i])) {
                return false;
            }
        }
        return true;
    }

    private Point findBlankBrick(int[][] status) {
        for (int i = 0; i < status.length; i++) {
            for (int j = 0; j < status[i].length; j++) {
                if (status[i][j] == GameActivity.BLANK_BRICK) {
                    return new Point(i, j);
                }
            }
        }
        return new Point(-1, -1);
    }

    private void generateRandomStatus() {
        if (mCurrBlankPos.x == -1 || mCurrBlankPos.y == -1) {
            return;
        }
        moveBlankBrick(DIRECTION_UP, false);
        moveBlankBrick(DIRECTION_LEFT, false);
        moveBlankBrick(DIRECTION_UP, false);
        moveBlankBrick(DIRECTION_UP, false);
        moveBlankBrick(DIRECTION_RIGHT, false);

        for (int i = 0; i < 50; i++) {
            int d = (int) (Math.random() * 4);
            switch (d) {
                case 0:
                    moveBlankBrick(DIRECTION_UP, false);
                    break;
                case 1:
                    moveBlankBrick(DIRECTION_DOWN, false);
                    break;
                case 2:
                    moveBlankBrick(DIRECTION_LEFT, false);
                    break;
                case 3:
                    moveBlankBrick(DIRECTION_RIGHT, false);
                    break;
            }
        }
    }

    private boolean moveBlankBrick(int direction, boolean shouldNotifyView) {
        Point p = mCurrBlankPos;
        int newX = p.x;
        int newY = p.y;
        if (direction == DIRECTION_UP || direction == DIRECTION_DOWN) {
            newY += direction == DIRECTION_UP ? -1 : 1;
        } else if (direction == DIRECTION_LEFT || direction == DIRECTION_RIGHT) {
            newX += direction == DIRECTION_LEFT ? -1 : 1;
        } else {
            return false;
        }
        if (newX >= mSpanCount || newX < 0 || newY >= mSpanCount || newY < 0) {
            return false;
        }

        int tInt = mCurrStatus[p.y][p.x];
        mCurrStatus[p.y][p.x] = mCurrStatus[newY][newX];
        mCurrStatus[newY][newX] = tInt;
//        int oldPos = p.y * mSpanCount + p.x;
//        int newPos = newY * mSpanCount + newX;
//        Bitmap tBitmap = mBitmapBricks[oldPos];
//        mBitmapBricks[oldPos] = mBitmapBricks[newPos];
//        mBitmapBricks[newPos] = tBitmap;

        if (shouldNotifyView) {
            int posA = p.y * mSpanCount + p.x;
            int posB = newY * mSpanCount + newX;
            if (posA > posB) {
                int t = posA;
                posA = posB;
                posB = t;
            }
            mRecyclerView.getAdapter().notifyItemMoved(posB, posA);
            mRecyclerView.getAdapter().notifyItemMoved(posA + 1, posB);
        }

        p.x = newX;
        p.y = newY;
        return true;
    }

    private int[][] copyStatus(int[][] status) {
        int[][] result = new int[status.length][];
        for (int i = 0; i < result.length; i++) {
            result[i] = status[i].clone();
        }
        return result;
    }
}
