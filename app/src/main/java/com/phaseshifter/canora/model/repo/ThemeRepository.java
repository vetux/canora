package com.phaseshifter.canora.model.repo;

import com.phaseshifter.canora.data.theme.AppTheme;

import java.util.List;

public interface ThemeRepository {
    List<AppTheme> getAll();

    AppTheme get(int id);
}