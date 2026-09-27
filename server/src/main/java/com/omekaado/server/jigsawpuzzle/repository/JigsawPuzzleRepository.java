package com.omekaado.server.jigsawpuzzle.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.omekaado.server.jigsawpuzzle.model.JigsawPuzzle;

public interface JigsawPuzzleRepository extends JpaRepository<JigsawPuzzle, Long>{ }