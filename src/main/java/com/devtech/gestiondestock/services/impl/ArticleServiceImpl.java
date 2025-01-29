package com.devtech.gestiondestock.services.impl;

import com.devtech.gestiondestock.dto.*;
import com.devtech.gestiondestock.exception.EntityNotFoundException;
import com.devtech.gestiondestock.exception.ErrorsCode;
import com.devtech.gestiondestock.exception.InvalidEntityException;
import com.devtech.gestiondestock.exception.InvalidOpperatioException;
import com.devtech.gestiondestock.model.Article;
import com.devtech.gestiondestock.model.Category;
import com.devtech.gestiondestock.repository.*;
import com.devtech.gestiondestock.services.ArticleService;
import com.devtech.gestiondestock.validator.ArticleValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author luca
 */
@Service
@Slf4j
@AllArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final String ARTICLE_ALREADY_USED = "Article already used";
    private static final String OPERATION_IMPOSSIBLE = "Operation impossible : une ou plusieur commande / vente existe deja pour cette article";
    private ArticleRepository articleRepository;
    private LigneVenteRepository venteRepository;
    private LigneCommandeClientRepository commandeClientRepository;
    private LigneCommandeFournisseurRepository commandeFournisseurRepository;
    private CategoryRepository categoryRepository;


    @Override
    public ArticleDto save(ArticleDto dto) {
        List<String> errors = ArticleValidator.validate(dto);
        if (!errors.isEmpty()) {
            log.error("Article is not valide {}", dto);
            throw new InvalidEntityException("L'article n'est pas valide", ErrorsCode.ARTICLE_NOT_VALID, errors);
        }
        Optional<Category> category = this.categoryRepository.findById(dto.getCategory().getId());
        if (!category.isPresent()){
            log.warn("Category with ID {} was not found in the DB", dto.getCategory().getId());
            throw new EntityNotFoundException(
                    "La categorie avec l'Id = "
                            + dto.getCategory().getId() +
                            " n'existe pas dans la BDD",
                    ErrorsCode.CATEGORY_NOT_FOUND
            );
        }
        dto.setCategory(CategoryDto.fromEntity(category.get()));
        return ArticleDto.fromEntity(
                this.articleRepository.save(ArticleDto.toEntity(dto))
        );
    }

    @Override
    public ArticleDto findById(Integer id) {
        checkIdArticle(id);
        Optional<Article> article = this.articleRepository.findById(id);
        return Optional.of(ArticleDto.fromEntity(article.get())).orElseThrow(() ->
                new EntityNotFoundException(
                        "Aucun article avec l'ID = " + id + " n'a ete trouver dans la base de donnee",
                        ErrorsCode.ARTICLE_NOT_FOUND
                )
        );
    }

    @Override
    public ArticleDto findByCodeArticle(String code) {
        if (!StringUtils.hasLength(code)){
            log.error("Article code is null");
            return null;
        }
        Optional<Article> article = this.articleRepository.findArticleByCodeArticle(code);
        return Optional.of(ArticleDto.fromEntity(article.get())).orElseThrow(() ->
                new EntityNotFoundException(
                        "Aucun article avec le code = " + code + " n'a ete trouver dans la base de donnee",
                        ErrorsCode.ARTICLE_NOT_FOUND
                )
        );
    }

    @Override
    public List<ArticleDto> findAll() {
        return this.articleRepository.findAll().stream()
                .map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<LigneVenteDto> findHistoriqueVente(Integer idArticle) {
        checkIdArticle(idArticle);
        return this.venteRepository.findAllByArticleId(idArticle)
                .stream().map(LigneVenteDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<LigneCommandeClientDto> findHistoriqueCommandeClient(Integer idArticle) {
        checkIdArticle(idArticle);
        return this.commandeClientRepository.findAllByArticleId(idArticle)
                .stream().map(LigneCommandeClientDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<LigneCommandeFournisseurDto> findHistoriqueCommandeFournisseur(Integer idArticle) {
        checkIdArticle(idArticle);
        return this.commandeFournisseurRepository.findAllByArticleId(idArticle)
                .stream().map(LigneCommandeFournisseurDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ArticleDto> findAllByCategorie(Integer idCategorie) {
        if (idCategorie == null){
            log.error("Category ID is null");
            throw new InvalidOpperatioException("Aucun article trouver avec un ID null",
                    ErrorsCode.CATEGORY_NOT_FOUND
            );
        }
        return this.articleRepository.findAllByCategoryId(idCategorie)
                .stream().map(ArticleDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Integer id) {
        checkArticleBeforDelete(id);
        this.articleRepository.deleteById(id);
    }

    private void checkIdArticle(Integer id) {
        if (id == null){
            log.error("Article ID is null");
            throw new EntityNotFoundException(
                    "L'id de l'article est null",
                    ErrorsCode.ID_NOT_VALID
            );
        }
    }

    private void checkArticleBeforDelete(Integer idArticle){


        if (!CollectionUtils.isEmpty(findHistoriqueCommandeClient(idArticle))) {
            log.error(ARTICLE_ALREADY_USED);
            throw new InvalidOpperatioException(OPERATION_IMPOSSIBLE,
                    ErrorsCode.ARTICLE_ALREADY_IN_USE
            );
        }

        if (!CollectionUtils.isEmpty(findHistoriqueCommandeFournisseur(idArticle))) {
            log.error(ARTICLE_ALREADY_USED);
            throw new InvalidOpperatioException(OPERATION_IMPOSSIBLE,
                    ErrorsCode.ARTICLE_ALREADY_IN_USE
            );
        }

        if (!CollectionUtils.isEmpty(findHistoriqueVente(idArticle))) {
            log.error(ARTICLE_ALREADY_USED);
            throw new InvalidOpperatioException(OPERATION_IMPOSSIBLE,
                    ErrorsCode.ARTICLE_ALREADY_IN_USE
            );
        }
    }
}
