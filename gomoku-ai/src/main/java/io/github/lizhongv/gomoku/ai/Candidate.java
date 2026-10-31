package io.github.lizhongv.gomoku.ai;

import io.github.lizhongv.gomoku.core.Move;

/**
 * 分析给出的候选着法。
 *
 * @param move 候选点
 * @param score 评分（当前行棋方视角）
 * @param depth 得到该评分时的搜索深度
 */
public record Candidate(Move move, int score, int depth) {}
