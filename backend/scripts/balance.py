"""
SmartSquad AI - Constraint-Satisfaction Team Balancer

Reads JSON from stdin, assigns players to balanced teams while respecting
must_be_with and cannot_be_with constraints. Uses backtracking search with
skill-balance optimization.

Input (stdin JSON):
{
  "players": [{"name": "matan", "skillLevel": 4, "secondarySkill": 3}, ...],
  "constraints": [{"type": "must_be_with", "playerA": "matan", "playerB": "yossi"}, ...],
  "numTeams": 3
}

Output (stdout JSON):
  Success: {"status": "success", "teams": [{"players": [...], "totalSkill": 9}, ...]}
  Error:   {"status": "error", "reason": "constraint_conflict", "conflicting_players": ["A","B"], "message": "..."}
"""

import sys
import json
from itertools import combinations


def build_constraint_groups(players, constraints):
    """
    Pre-process constraints into lookup structures.
    Returns (must_be_with_groups, cannot_be_with_pairs).

    must_be_with_groups: list of sets, each set contains players that must be together.
    cannot_be_with_pairs: list of (playerA, playerB) that must be on different teams.
    """
    name_set = {p["name"].lower() for p in players}

    # Union-Find for must_be_with
    parent = {}
    for p in players:
        parent[p["name"].lower()] = p["name"].lower()

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(a, b):
        ra, rb = find(a), find(b)
        if ra != rb:
            parent[ra] = rb

    cannot_pairs = []

    for c in constraints:
        a = c["playerA"].lower()
        b = c["playerB"].lower()
        if a not in name_set or b not in name_set:
            continue

        if c["type"] == "must_be_with":
            union(a, b)
        elif c["type"] == "cannot_be_with":
            cannot_pairs.append((a, b))

    # Check for contradictions: two players must be together AND cannot be together
    for a, b in cannot_pairs:
        if find(a) == find(b):
            return None, None, (a, b)

    # Build groups from union-find
    groups = {}
    for name in name_set:
        root = find(name)
        if root not in groups:
            groups[root] = set()
        groups[root].add(name)

    group_list = list(groups.values())

    return group_list, cannot_pairs, None


def solve(groups, cannot_pairs, num_teams, player_skills):
    """
    Backtracking solver that assigns groups to teams.
    Optimizes for minimal skill difference between teams.
    """
    max_players = sum(len(g) for g in groups)
    max_per_team = -(-max_players // num_teams)  # ceil division

    # Sort groups by total skill descending (assign hardest first)
    groups.sort(key=lambda g: sum(player_skills.get(n, 0) for n in g), reverse=True)

    # Build cannot_be_with lookup: group_index -> set of group_indices it conflicts with
    group_index_for_player = {}
    for i, g in enumerate(groups):
        for name in g:
            group_index_for_player[name] = i

    group_conflicts = {}
    for a, b in cannot_pairs:
        gi_a = group_index_for_player.get(a)
        gi_b = group_index_for_player.get(b)
        if gi_a is not None and gi_b is not None and gi_a != gi_b:
            group_conflicts.setdefault(gi_a, set()).add(gi_b)
            group_conflicts.setdefault(gi_b, set()).add(gi_a)

    team_sizes = [0] * num_teams
    team_skills = [0] * num_teams
    assignment = [None] * len(groups)

    best = {"score": float("inf"), "assignment": None}

    def group_skill(g):
        return sum(player_skills.get(n, 0) for n in g)

    def score():
        return max(team_skills) - min(team_skills)

    def backtrack(idx):
        if idx == len(groups):
            s = score()
            if s < best["score"]:
                best["score"] = s
                best["assignment"] = assignment[:]
            return

        g = groups[idx]
        g_size = len(g)
        g_skill = group_skill(g)

        # Determine which teams are forbidden due to cannot_be_with
        forbidden = set()
        for conflict_idx in group_conflicts.get(idx, set()):
            if assignment[conflict_idx] is not None:
                forbidden.add(assignment[conflict_idx])

        # Try each team, ordered by current skill (weakest first for pruning)
        team_order = sorted(range(num_teams), key=lambda t: team_skills[t])

        for t in team_order:
            if t in forbidden:
                continue
            if team_sizes[t] + g_size > max_per_team:
                continue

            # Prune: if current partial score already worse than best, skip
            team_skills[t] += g_skill
            if best["assignment"] is not None and score() >= best["score"]:
                team_skills[t] -= g_skill
                continue

            assignment[idx] = t
            team_sizes[t] += g_size

            backtrack(idx + 1)

            assignment[idx] = None
            team_sizes[t] -= g_size
            team_skills[t] -= g_skill

        # If no team worked for this group, we've hit a dead end (backtrack handles it)

    backtrack(0)
    return best["assignment"]


def main():
    try:
        raw = sys.stdin.read()
        data = json.loads(raw)
    except Exception as e:
        print(json.dumps({"status": "error", "reason": "invalid_input", "message": str(e)}))
        sys.exit(0)

    players = data.get("players", [])
    constraints = data.get("constraints", [])
    num_teams = data.get("numTeams", 3)

    if len(players) < num_teams:
        print(json.dumps({
            "status": "error",
            "reason": "not_enough_players",
            "message": f"Need at least {num_teams} players for {num_teams} teams, got {len(players)}"
        }))
        sys.exit(0)

    # Build skill lookup
    player_skills = {}
    for p in players:
        player_skills[p["name"].lower()] = p.get("skillLevel", 3)

    # Build constraint groups
    groups, cannot_pairs, contradiction = build_constraint_groups(players, constraints)

    if contradiction:
        a, b = contradiction
        print(json.dumps({
            "status": "error",
            "reason": "constraint_conflict",
            "conflicting_players": [a, b],
            "message": f"Players '{a}' and '{b}' have contradictory constraints: "
                       f"they must be together AND cannot be together."
        }))
        sys.exit(0)

    # Check if any group is too large for a team
    max_per_team = -(-len(players) // num_teams)
    for g in groups:
        if len(g) > max_per_team:
            print(json.dumps({
                "status": "error",
                "reason": "constraint_conflict",
                "conflicting_players": list(g),
                "message": f"A must-be-with group of {len(g)} players exceeds the team limit of {max_per_team}."
            }))
            sys.exit(0)

    # Solve
    result = solve(groups, cannot_pairs, num_teams, player_skills)

    if result is None:
        # Find which cannot_be_with pair is most likely the problem
        conflicting = list(cannot_pairs[0]) if cannot_pairs else []
        print(json.dumps({
            "status": "error",
            "reason": "constraint_conflict",
            "conflicting_players": conflicting,
            "message": "No valid team assignment exists with the given constraints."
        }))
        sys.exit(0)

    # Build output teams
    player_lookup = {p["name"].lower(): p for p in players}
    teams = [{"players": [], "totalSkill": 0} for _ in range(num_teams)]

    for group_idx, team_idx in enumerate(result):
        for player_name in groups[group_idx]:
            p = player_lookup[player_name]
            teams[team_idx]["players"].append(p)
            teams[team_idx]["totalSkill"] += p.get("skillLevel", 0)

    print(json.dumps({"status": "success", "teams": teams}))


if __name__ == "__main__":
    main()
